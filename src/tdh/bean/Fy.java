package tdh.bean;

public class Fy {
	
	private String fydm;
	private String fjm;
	private String dm;
	private String fymc;
	
	public String getFydm() {
		return fydm;
	}
	public void setFydm(String fydm) {
		this.fydm = fydm;
	}
	public String getFjm() {
		return fjm;
	}
	public void setFjm(String fjm) {
		this.fjm = fjm;
	}
	public String getDm() {
		return dm;
	}
	public void setDm(String dm) {
		this.dm = dm;
	}
	public String getFymc() {
		return fymc;
	}
	public void setFymc(String fymc) {
		this.fymc = fymc;
	}
	@Override
	public String toString() {
		return "Fy [fydm=" + fydm + ", fjm=" + fjm + ", dm=" + dm + ", fymc=" + fymc + "]";
	}
	
}
