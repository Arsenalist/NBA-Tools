import org.jsoup.*;
import org.jsoup.select.*;
import java.util.*;
import org.jsoup.nodes.*;
import static java.lang.Double.*;
import freemarker.template.*;
import java.io.*;
import org.json.*;
import org.apache.commons.io.*;
import java.net.*;
import java.text.*;

public class NBA {

	public String preview(String team) throws Exception {

	    Configuration cfg = new Configuration();
	    
	    // Where do we load the templates from:
	    cfg.setClassForTemplateLoading(NBA.class, "templates");    
	    // Some other recommended settings:
	    cfg.setIncompatibleImprovements(new Version(2, 3, 20));
	    cfg.setDefaultEncoding("UTF-8");
	    cfg.setLocale(Locale.US);
	    cfg.setTemplateExceptionHandler(TemplateExceptionHandler.RETHROW_HANDLER);
	    Template template = cfg.getTemplate("preview.html");

	   	String otherTeam = getNextOpponent(team);
	    int scoreId = getScoreIdFromBrefCode(team);
	    int otherScoreId = getScoreIdFromBrefCode(otherTeam);
	    String awayTeam, homeTeam;
		String nextGameUri = getNextGameUri(scoreId);
		GameInfo gameInfo = getGameInfo(nextGameUri);
	    int awayScoreId = gameInfo.awayId; 
	    int homeScoreId = gameInfo.homeId;
	    if (scoreId == awayScoreId) {
	    	awayTeam = team;
	    	homeTeam = otherTeam;
	    } else {
	    	homeTeam = team;
	    	awayTeam = otherTeam;
	    }

	    Map<String, Object> data = new HashMap<String, Object>();
	    Map<String, Object> home = new HashMap<String, Object>();
	    Map<String, Object> away = new HashMap<String, Object>();



		away.put("results", getLatestResults(awayScoreId));
		away.put("leaders", findLeaders(awayTeam));
		Map<String, TeamSeason> teamStats = getTeamStats();
		away.put("rank", teamStats.get(awayTeam));
		away.put("standings", getStandings(awayTeam));
		
		home.put("results", getLatestResults(homeScoreId));
		home.put("leaders", findLeaders(homeTeam));
		Map<String, TeamSeason> teamStats2 = getTeamStats();
		home.put("rank", teamStats2.get(homeTeam));
		home.put("standings", getStandings(homeTeam));


		data.put("away", away);
		data.put("home", home);
		data.put("headToHead", headToHead(team, otherTeam));
		data.put("gameInfo", gameInfo);



	    // Write output to the console
	    StringWriter writer = new StringWriter();
	    template.process(data, writer);
	    return writer.getBuffer().toString();

	}

	public GameInfo getGameInfo(String uri) throws Exception {
		// location, stadium, tv_listings.[short_name]
		String url = "http://api.thescore.com" + uri;
		String str = IOUtils.toString(new URL(url), "UTF-8");
		JSONObject game = new JSONObject(str);
		GameInfo gameInfo = new GameInfo();
		gameInfo.location = game.getString("location");
		gameInfo.stadium = game.getString("stadium");
		JSONArray tvListings = game.getJSONArray("tv_listings");
		String tv = "";
		for (int i=0; i<tvListings.length(); i++) {
			tv += tvListings.getJSONObject(i).getString("short_name");
			if (i != tvListings.length()-1) {
				tv += ", ";
			}
		}
		gameInfo.tv = tv;
		SimpleDateFormat sdf = new SimpleDateFormat("EEE, d MMM yyyy HH:mm:ss Z");
		sdf.setTimeZone(TimeZone.getTimeZone("America/New_York"));
		gameInfo.time = new SimpleDateFormat("EEE MMM d, h:mm a").format(sdf.parse(game.getString("game_date"))) + " EST";
		gameInfo.awayLogo = game.getJSONObject("away_team").getJSONObject("logos").getString("small");
		gameInfo.awayName = game.getJSONObject("away_team").getString("full_name");
		gameInfo.awayId = game.getJSONObject("away_team").getInt("id");
		gameInfo.homeLogo = game.getJSONObject("home_team").getJSONObject("logos").getString("small");
		gameInfo.homeName = game.getJSONObject("home_team").getString("full_name");
		gameInfo.homeId = game.getJSONObject("home_team").getInt("id");

        if (!game.isNull("odd")) {
            gameInfo.line = game.getJSONObject("odd").getString("line");
            gameInfo.overUnder = game.getJSONObject("odd").getString("over_under");
        }



		return gameInfo;
	}

	public String getNextGameUri(int teamId) throws Exception {
		String url = "http://api.thescore.com/nba/teams/" + teamId + "/events/upcoming?rpp=2";
		String str = IOUtils.toString(new URL(url), "UTF-8");
		JSONArray upcoming = new JSONArray(str);
		if (upcoming.length() > 0) {
			for (int i=0; i<upcoming.length(); i++) {
				JSONObject game = upcoming.getJSONObject(i);
				if (game.getString("event_status").equals("pre_game")) {
					return game.getString("api_uri");
				}
			}
		}
		return null;

	}

	public int getScoreIdFromBrefCode(String team) throws Exception {
		return Integer.parseInt(ResourceBundle.getBundle("bref_to_thescore").getString(team));
	}
    public String getBrefCodeFromScoreId(int id) throws Exception {
        return ResourceBundle.getBundle("thescore_to_bref").getString(id + "");
    }

	public String getNextOpponent(String team) throws Exception {
        int teamId = getScoreIdFromBrefCode(team);
        String url = "http://api.thescore.com/nba/teams/" + teamId + "/events/upcoming?rpp=2";
        String str = IOUtils.toString(new URL(url), "UTF-8");
        JSONArray upcoming = new JSONArray(str);
        if (upcoming.length() > 0) {
            for (int i=0; i<upcoming.length(); i++) {
                JSONObject game = upcoming.getJSONObject(i);
                if (game.getString("event_status").equals("pre_game")) {
                    int away = game.getJSONObject("away_team").getInt("id");
                    int home = game.getJSONObject("home_team").getInt("id");
                    int answer = away == teamId ? home : away;
                    return getBrefCodeFromScoreId(answer);
                }
            }
        }
        return null;


    }

	public List<TeamResult> getLatestResults(int teamId) throws Exception {
		String url = "https://api.thescore.com/nba/teams/" + teamId + "/events/previous?rpp=5";

		String str = IOUtils.toString(new URL(url), "UTF-8");
		JSONArray games = new JSONArray(str);
		List<TeamResult> teamResults = new ArrayList<TeamResult>();
		for (int i=0; i < games.length(); i++) {
			TeamResult tr = new TeamResult();
			JSONObject game = games.getJSONObject(i);
			JSONObject away = game.getJSONObject("away_team");
			JSONObject home = game.getJSONObject("home_team");
			tr.homeId = home.getInt("id");
			tr.home = home.getString("abbreviation");
			tr.homeScore = "" + game.getJSONObject("box_score").getJSONObject("score").getJSONObject("home").getInt("score");
			tr.awayId = away.getInt("id");
			tr.away = away.getString("abbreviation");
			tr.awayScore = "" + game.getJSONObject("box_score").getJSONObject("score").getJSONObject("away").getInt("score");

			SimpleDateFormat sdf = new SimpleDateFormat("EEE, d MMM yyyy HH:mm:ss Z");
			sdf.setTimeZone(TimeZone.getTimeZone("America/New_York"));
			tr.gameDate = new SimpleDateFormat("MMM d").format(sdf.parse(game.getString("game_date")));
			tr.boxScoreUrl = "http://www.thescore.com" + game.getString("api_uri") + "/box_score";
			teamResults.add(tr);
		}
		return teamResults;

	}
/*
	public List<TeamResult> getLatestResults(String team) throws Exception {
		String url = "http://www.basketball-reference.com/teams/" + team + "/2015/gamelog/";
		Document doc = Jsoup.connect(url).get();
		Element table = doc.getElementById("tgl_basic");
		Elements results = table.getElementsByAttributeValueStarting("id", "tgl_basic.");
		int startIndex = -1, endIndex = -1;
		int numGames = 5;
		if (results.size() >= numGames) {
			startIndex = results.size() - numGames;
			endIndex = results.size()-1;
		} else if (results.size() > 0) {
			startIndex = 0;
			endIndex = results.size()-1;
		}
		List<TeamResult> teamResults = new ArrayList<TeamResult>();
		if (startIndex != -1) {
			for (int i=startIndex; i<=endIndex; i++) {
				TeamResult tr = new TeamResult();
				Element result = results.get(i);
				String location = result.child(3).text();
				if (location.equals("@")) {
					tr.home = result.child(4).text();
					tr.homeScore = result.child(7).text();
					tr.away = team;
					tr.awayScore = result.child(6).text();
				} else {
					tr.away = result.child(4).text();
					tr.awayScore = result.child(7).text();
					tr.home = team;
					tr.homeScore = result.child(6).text();
				}
				tr.gameDate = result.child(2).text();
				tr.boxScoreUrl = "http://basketball-reference.com" + result.child(2).child(0).attr("href");
				teamResults.add(tr);
			}
		}
		Collections.reverse(teamResults);
		return teamResults;
	}
*/
	public PlayerSeason topPlayerSeason(List<PlayerSeason> list, String field) {
		Collections.sort(list, new PlayerSeasonComparator(field));
		return list.get(0);
	}
	public Map<String, PlayerSeason> findLeaders(String team) throws Exception {
		Map<String, PlayerSeason> leaders = new HashMap<String, PlayerSeason>();
		List<PlayerSeason> stats = getSeasonStats(team);
		leaders.put("ppg", topPlayerSeason(stats, "ppg"));
		leaders.put("rpg", topPlayerSeason(stats, "rpg"));
		leaders.put("apg", topPlayerSeason(stats, "apg"));
		leaders.put("spg", topPlayerSeason(stats, "spg"));
		leaders.put("bpg", topPlayerSeason(stats, "bpg"));
		return leaders;
	}

	public List<PlayerSeason> getSeasonStats(String team) throws Exception {
		String url = "http://www.basketball-reference.com/teams/" + team + "/2015.html";
		Document doc = Jsoup.connect(url).get();
		Element table = doc.getElementById("per_game");
		Elements results = table.child(2).getElementsByTag("tr");
		List<PlayerSeason> playerSeasons = new ArrayList<PlayerSeason>();
		for (Element r : results) {
			//1, 20 21, 22 27
			PlayerSeason ps = new PlayerSeason();
			ps.name = r.child(1).text();
			ps.ppg = parseDouble(r.child(26).text());
			ps.rpg = parseDouble(r.child(20).text());
			ps.apg = parseDouble(r.child(21).text());
			ps.spg = parseDouble(r.child(22).text());
			ps.bpg = parseDouble(r.child(23).text());
			playerSeasons.add(ps);
		}
		return playerSeasons;
	}

	public void applyRankings(List<TeamSeason> teamSeasons) {

		Collections.sort(teamSeasons, new TeamSeasonComparator("ortg"));
		for (int i=0; i<teamSeasons.size(); i++) {
			teamSeasons.get(i).ortgRank = i+1;
		}

		Collections.sort(teamSeasons, new TeamSeasonComparator("drtg"));
		for (int i=0; i<teamSeasons.size(); i++) {
			teamSeasons.get(i).drtgRank = i+1;
		}

		Collections.sort(teamSeasons, new TeamSeasonComparator("pace"));
		for (int i=0; i<teamSeasons.size(); i++) {
			teamSeasons.get(i).paceRank = i+1;
		}

		Collections.sort(teamSeasons, new TeamSeasonComparator("drb"));
		for (int i=0; i<teamSeasons.size(); i++) {
			teamSeasons.get(i).drbRank = i+1;
		}

        Collections.sort(teamSeasons, new TeamSeasonComparator("ts"));
        for (int i=0; i<teamSeasons.size(); i++) {
            teamSeasons.get(i).tsRank = i+1;
        }
	}

	public Map<String, TeamSeason> getTeamStats() throws Exception {
		String url = "http://www.basketball-reference.com/leagues/NBA_2015.html";
		Document doc = Jsoup.connect(url).get();
		Element table = doc.getElementById("misc");
		Elements results = table.child(2).getElementsByTag("tr");
		results.remove(results.size()-1); // get rid of average
		List<TeamSeason> teamSeasons = new ArrayList<TeamSeason>();
		for (Element r : results) {
			//1, 20 21, 22 27
				TeamSeason teamSeason = new TeamSeason();
				teamSeason.name = r.child(1).child(0).attr("href").split("/")[2];
				teamSeason.ortg = parseDouble(r.child(8).text());
				teamSeason.drtg = parseDouble(r.child(9).text());
				teamSeason.pace = parseDouble(r.child(10).text());
				teamSeason.drb = parseDouble(r.child(20).text());
                teamSeason.ts = parseDouble(r.child(13).text());
				teamSeasons.add(teamSeason);
		}
		applyRankings(teamSeasons);
		Map<String, TeamSeason> teamSeasonMap = new HashMap<String, TeamSeason>();
		for (TeamSeason ts : teamSeasons) {
			teamSeasonMap.put(ts.name, ts);
		}

		return teamSeasonMap;
	}


	public Standings getStandings(String teamCode) throws Exception {
        int id = getScoreIdFromBrefCode(teamCode);
        String url = "http://api.thescore.com/nba/teams/" + id + "/";
        String str = IOUtils.toString(new URL(url), "UTF-8");
        JSONObject team = new JSONObject(str);
        Standings standings = new Standings();
        standings.division = team.getString("division");
        standings.conference = team.getString("conference");
        standings.conferenceStanding = team.getJSONObject("standing").getInt("conference_ranking");
        standings.divisionStanding = team.getJSONObject("standing").getInt("division_ranking");
        standings.recentRecord = team.getJSONObject("standing").getString("last_ten_games_record");
        standings.record = team.getJSONObject("standing").getString("short_record");
        return standings;
	}

	public List<TeamResult> headToHead(String awayTeam, String homeTeam) throws Exception {
        int team1 = getScoreIdFromBrefCode(homeTeam);
        int team2 = getScoreIdFromBrefCode(awayTeam);
        String url = "https://api.thescore.com/nba/teams/" + team1 + "/events/previous?rpp=200";
        List<TeamResult> teamResults = new ArrayList<TeamResult>();
        String str = IOUtils.toString(new URL(url), "UTF-8");
        JSONArray games = new JSONArray(str);
        List<JSONObject> selectedGames = new ArrayList<JSONObject>();
        for (int i=0; i < games.length(); i++) {
            TeamResult tr = new TeamResult();
            JSONObject game = games.getJSONObject(i);
            int awayId = game.getJSONObject("away_team").getInt("id");
            int homeId = game.getJSONObject("home_team").getInt("id");
            if ((team1 == homeId && team2 == awayId) || (team2 == homeId && team1 == awayId)) {
                selectedGames.add(game);
                if (selectedGames.size() == 5) {
                    break;
                }
            }
        }
        for (JSONObject game : selectedGames) {
            JSONObject away = game.getJSONObject("away_team");
            JSONObject home = game.getJSONObject("home_team");

            TeamResult tr = new TeamResult();
            tr.homeId = home.getInt("id");
            tr.home = home.getString("abbreviation");
            tr.homeScore = "" + game.getJSONObject("box_score").getJSONObject("score").getJSONObject("home").getInt("score");
            tr.awayId = away.getInt("id");
            tr.away = away.getString("abbreviation");
            tr.awayScore = "" + game.getJSONObject("box_score").getJSONObject("score").getJSONObject("away").getInt("score");

            SimpleDateFormat sdf = new SimpleDateFormat("EEE, d MMM yyyy HH:mm:ss Z");
            sdf.setTimeZone(TimeZone.getTimeZone("America/New_York"));
            tr.gameDate = new SimpleDateFormat("MMM d").format(sdf.parse(game.getString("game_date")));
            tr.boxScoreUrl = "http://www.thescore.com" + game.getString("api_uri") + "/box_score";
            teamResults.add(tr);
        }
        return teamResults;
    }


/*

        System.out.println(home + " " + away);
		String url = "http://www.basketball-reference.com/play-index/tgl_finder.cgi?request=1&match=game&lg_id=NBA&year_min=1947&year_max=2015&team_id=" + away + "&opp_id=" + home + "&is_playoffs=&round_id=&best_of=&team_seed_cmp=eq&team_seed=&opp_seed_cmp=eq&opp_seed=&is_range=N&game_num_type=team&game_num_min=&game_num_max=&game_month=&game_location=&game_result=&is_overtime=&c1stat=&c1comp=gt&c1val=&c2stat=&c2comp=gt&c2val=&c3stat=&c3comp=gt&c3val=&c4stat=&c4comp=gt&c4val=&order_by=date_game";
		Document doc = Jsoup.connect(url).get();
		Element table = doc.getElementById("stats");
		Elements results = table.child(2).getElementsByTag("tr");

		int startIndex = -1, endIndex = -1;
		int numGames = 5;
		if (results.size() > 0) {
			startIndex = 0;
		}
		if (results.size() >= numGames) {
			endIndex = numGames;
		} else  if (results.size() != 0 && results.size() < numGames) {
			endIndex = results.size() - 1;
		}
		if (startIndex == -1)
			return null;

		// 0, 3, 19, 32
		List<TeamResult> teamResults = new ArrayList<TeamResult>();
		for (int i=startIndex; i<=endIndex; i++) {

			Element result = results.get(i);
			String location = result.child(3).text().trim();

			String score1 = result.child(19).text();
			String score2 = result.child(32).text();

			TeamResult tr = new TeamResult();
			if (location.equals("@")) {
				tr.away = away;
				tr.home = home;
				tr.homeScore = score1;
				tr.awayScore = score2;
			} else {
				tr.away = home;
				tr.home = away;
				tr.homeScore = score2;
				tr.awayScore = score1;
			}
			tr.boxScoreUrl = "http://basketball-reference.com" + result.child(1).child(0).attr("href");
			tr.gameDate = result.child(1).text();
			teamResults.add(tr);
*/
}
