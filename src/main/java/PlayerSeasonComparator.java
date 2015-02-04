import java.util.Comparator;

public class PlayerSeasonComparator implements Comparator<PlayerSeason> {
	String sortField;
	public PlayerSeasonComparator(String sortField) {
		this.sortField = sortField;
	}

    @Override
    public int compare(PlayerSeason o1, PlayerSeason o2) {
    	if (sortField.equals("ppg")) {
        	return o2.ppg.compareTo(o1.ppg);
    	} else if (sortField.equals("rpg")) {
        	return o2.rpg.compareTo(o1.rpg);
    	} else if (sortField.equals("apg")) {
        	return o2.apg.compareTo(o1.apg);
    	} else if (sortField.equals("spg")) {
        	return o2.spg.compareTo(o1.spg);
    	} else if (sortField.equals("bpg")) {
        	return o2.bpg.compareTo(o1.bpg);
        } else {
        	return 0;
        }
    }
}
