package com.hoonit.xeye.test;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.util.Calendar;
import java.util.Random;
import java.util.StringTokenizer;

import org.apache.commons.lang.StringUtils;

import com.hoonit.xeye.util.ByteUtils;
import com.hoonit.xeye.util.HexUtils;
import com.hoonit.xeye.util.Utils;

public class Temp {

	public static void main(String[] args){
		
		/*String data = "23897";
		
		BigDecimal bd1 = new BigDecimal(data);
		BigDecimal bd2 = new BigDecimal("0.01");
		
		String result = bd1.multiply(bd2).toString();
		
		if(result.contains(".")){
			
			StringTokenizer st = new StringTokenizer(result, ".");
			
			String val1 = st.nextToken();
			String val2 = st.nextToken();
			
			int g = Integer.parseInt(val1);
			
			byte v = (byte)g;
			
			System.out.println(v);
			
			//byte[] bytes = ByteUtils.toBytes(Integer.parseInt(val1));
			
			//System.out.println(ByteUtils.toHexString(bytes[bytes.length-1]));
			
			//System.out.println(ByteUtils.toByteObject(val1, (byte)0x00));
			
		}else{
			result = "0";
		}*/
		
		/*String version = "1.0";
		
		BigDecimal bd1 = new BigDecimal(version);
		BigDecimal bd2 = new BigDecimal("10");
		
		int result = bd1.multiply(bd2).intValue();
		
		System.out.println((byte)result);*/
		
		/*String t = "한글";
		
		System.out.println(t.getBytes().length);
		
		byte[] b = new byte[10];
		
		b[0] = t.getBytes()[0];
		b[1] = t.getBytes()[1];
		b[2] = t.getBytes()[2];
		b[3] = t.getBytes()[3];
		b[4] = t.getBytes()[4];
		b[5] = t.getBytes()[5];
		b[6] = 0x00;
		b[7] = 0x00;
		b[8] = 0x00;
		b[9] = 0x00;
		
		try{
			System.out.println(new String(b, "UTF-8").trim());
		}catch(Exception e){
			
		}*/
		
		/*try{
			
			byte[] b = Utils.getFillNullByte("한글입니다".getBytes(), 10);
			
			System.out.println(b.length);
			
			System.out.println(new String(b, "UTF-8"));
		}catch(Exception e){
			e.printStackTrace();
		}*/
		
		/*byte version = 10;
		
		BigDecimal bd1 = new BigDecimal(version);
		BigDecimal bd2 = new BigDecimal("10");
		
		System.out.println(bd1.divide(bd2).floatValue());*/
		
		/*String t = "063456";
		
		System.out.println(t.substring(0, 2));
		System.out.println(t.substring(2, 4));
		System.out.println(t.substring(4, 6));
		
		System.out.println(ByteUtils.toByte(t.substring(0, 2), (byte)0x00));*/
		
		//System.out.println(ByteUtils.toHexString((byte)-1));
		//System.out.println((byte)0x00);
		
		/*Calendar cal = Calendar.getInstance();
		int year   = cal.get(Calendar.YEAR);
		int month  = cal.get(Calendar.MONTH) + 1;
		int date   = cal.get(Calendar.DATE);
		int hour   = cal.get(Calendar.HOUR_OF_DAY);
		int minute = cal.get(Calendar.MINUTE);
		int second = cal.get(Calendar.SECOND);
		
		System.out.println(year);
		System.out.println(month);
		System.out.println(date);
		System.out.println(hour);
		System.out.println(minute);
		System.out.println(second);*/
		
		/*Calendar cal = Calendar.getInstance();
		int min = cal.get(Calendar.MINUTE);
		
		if(min > 0 && min < 5){
			cal.add(Calendar.MINUTE, (5-min));
		}else if(min > 5 && min < 10){
			cal.add(Calendar.MINUTE, (10-min));
		}else if(min > 10 && min < 15){
			cal.add(Calendar.MINUTE, (15-min));
		}else if(min > 15 && min < 20){
			cal.add(Calendar.MINUTE, (20-min));
		}else if(min > 20 && min < 25){
			cal.add(Calendar.MINUTE, (25-min));
		}else if(min > 25 && min < 30){
			cal.add(Calendar.MINUTE, (30-min));
		}else if(min > 30 && min < 35){
			cal.add(Calendar.MINUTE, (35-min));
		}else if(min > 35 && min < 40){
			cal.add(Calendar.MINUTE, (40-min));
		}else if(min > 40 && min < 45){
			cal.add(Calendar.MINUTE, (45-min));
		}else if(min > 45 && min < 50){
			cal.add(Calendar.MINUTE, (50-min));
		}else if(min > 50 && min < 55){
			cal.add(Calendar.MINUTE, (55-min));
		}else if(min > 55 && min < 60){
			cal.add(Calendar.MINUTE, (60-min));
		}

		System.out.println(cal.get(Calendar.MINUTE));*/
		
		//System.out.println(Integer.parseInt("-9999") / 100);
		
		//DecimalFormat format = new DecimalFormat("###");
		//System.out.println(format.format(Double.parseDouble("2354") / 100.0D));
		//System.out.println(format.format(-99.99));
		
		//System.out.println(Math.floor(-99.99));
		
		//BigDecimal bd1 = new BigDecimal("-9999");
		//BigDecimal bd2 = bd1.divide(new BigDecimal("100"), 2, BigDecimal.ROUND_FLOOR);
		//System.out.println(bd2);
		//System.out.println(format.format(bd2.toString()));
		
		//System.out.println(Double.parseDouble("-9999") / 100.0D);
		
		//System.out.println(Double.parseDouble("9995") / 100.0D);
		//System.out.println(String.format("%.1f", Double.parseDouble("9994") / 100.0D));
		
		//String s = String.valueOf(Double.parseDouble("9995") / 100.0D);
		//System.out.println(s);
		//DecimalFormat df = new DecimalFormat(".##");
		//System.out.println(df.format(s));
		
		//System.out.println( (2356+2401) / 2);
		
		/*long ch1PowerSum = 11;
		long contractPower = 20;
		
		System.out.println((double)ch1PowerSum / (double)contractPower);
		
		double rate = ((double)ch1PowerSum / (double)contractPower) * 100.0D;
		
		System.out.println( Math.round(rate) );*/
		
		//Random random = new Random();
		//System.out.println(random.nextInt(60));
		
		//String[] sunRiset = "061011:161512".split(":");
		//System.out.println(sunRiset[0]);
		//System.out.println(sunRiset[1]);
		
		//System.out.println(10 % 5);
		
		/*DecimalFormat df = new DecimalFormat();
		df.applyPattern(".0");
		
		short s = 235;
		
		System.out.println(df.format(s / 100.0D));*/
		
		//System.out.println(Float.parseFloat(StringUtils.defaultIfEmpty(String.valueOf("21000.00"), "0")));
		//System.out.println(Long.parseLong(StringUtils.defaultIfEmpty(String.valueOf("21000.00"), "0")));
		//System.out.println(new BigDecimal(StringUtils.defaultIfEmpty(String.valueOf("21000.00"), "0")).longValue());
		
		StringTokenizer st = new StringTokenizer("05:43", ":");
		
		System.out.println(ByteUtils.toByte(st.nextToken(), (byte)0xFF));
		System.out.println(st.nextToken());
	}
}
