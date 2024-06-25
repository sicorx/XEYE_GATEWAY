package com.hoonit.xeye.manager;

import org.apache.log4j.Logger;

/**
 * 일출/일몰 시간, 계약전력 등의 정보를 관리하는 객체
 * @author Administrator
 *
 */
public class InformationManager {

	protected final Logger logger = Logger.getLogger(getClass().getName());
	
	private static InformationManager instance = new InformationManager();
	
	// 일출/일몰 관련 변수
	private String sunRiseTime;
	private String sunRiseMinute;
	private String sunSetTime;
	private String sunSetMinute;
	
	private InformationManager(){
	}
	
	public static InformationManager getInstance(){
		
		if(instance == null){
			instance = new InformationManager();
		}
		
		return instance;
	}

	public String getSunRiseTime() {
		return sunRiseTime;
	}

	public void setSunRiseTime(String sunRiseTime) {
		this.sunRiseTime = sunRiseTime;
	}

	public String getSunRiseMinute() {
		return sunRiseMinute;
	}

	public void setSunRiseMinute(String sunRiseMinute) {
		this.sunRiseMinute = sunRiseMinute;
	}

	public String getSunSetTime() {
		return sunSetTime;
	}

	public void setSunSetTime(String sunSetTime) {
		this.sunSetTime = sunSetTime;
	}

	public String getSunSetMinute() {
		return sunSetMinute;
	}

	public void setSunSetMinute(String sunSetMinute) {
		this.sunSetMinute = sunSetMinute;
	}
}
