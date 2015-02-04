public class PlayerSeason {
	public String name;
	public Double ppg, rpg, apg, spg, bpg;
	public String getName() {
		return name;
	}
	public Double getPpg() {
		return ppg;
	}
	public Double getRpg() {
		return rpg;
	}
	public Double getApg() {
		return apg;
	}
	public Double getSpg() {
		return spg;
	}
	public Double getBpg() {
		return bpg;
	}
	public String toString() {
		return name + ": "  + ppg + " pts " + rpg  + " reb "+ apg  + " ast "+ spg  + " stl "+ bpg + " blk";
	}
}
