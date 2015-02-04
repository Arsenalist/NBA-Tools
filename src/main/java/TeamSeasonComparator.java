import java.util.Comparator;

public class TeamSeasonComparator implements Comparator<TeamSeason> {
	String sortField;
	public TeamSeasonComparator(String sortField) {
		this.sortField = sortField;
	}

    @Override
    public int compare(TeamSeason o1, TeamSeason o2) {
    	if (sortField.equals("ortg")) {
        	return o2.ortg.compareTo(o1.ortg);
    	} else if (sortField.equals("drtg")) {
        	return o2.drtg.compareTo(o1.drtg);
    	} else if (sortField.equals("pace")) {
        	return o2.pace.compareTo(o1.pace);
    	} else if (sortField.equals("drb")) {
        	return o2.drb.compareTo(o1.drb);
        } else if (sortField.equals("ts")) {
            return o2.ts.compareTo(o1.ts);
        } else {
        	return 0;
        }	    	
    }
}
