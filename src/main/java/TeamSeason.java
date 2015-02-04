public class TeamSeason {
	public String name;
	public Double ortg, drtg, pace, drb, ts;
	public int ortgRank, drtgRank, paceRank, drbRank, tsRank;
	public String toString() {
		return name + " "  + ortg + " ORTG (" + ortgRank + ") " + drtg + " DRTG (" + drtgRank + ") " + pace + " Pace (" + paceRank + ") " + drb + " DRB% (" + drbRank + ")";
	}

	public String getName() {
		return  name;
	}
	public Double getOrtg() {
		return ortg;
	}
	public Double getDrtg() {
		return drtg;
	}
	public Double getPace() {
		return pace;
	}
	public Double getDrb() {
		return drb;
	}
	public int getOrtgRank() {
		return ortgRank;
	}
	public int getDrtgRank() {
		return drtgRank;
	}
	public int getPaceRank() {
		return paceRank;
	}
	public int getDrbRank() {
		return drbRank;
	}

    public Double getTs() {
        return ts;
    }

    public int getTsRank() {
        return tsRank;
    }
}
