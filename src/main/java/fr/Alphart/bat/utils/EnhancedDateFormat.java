package fr.Alphart.bat.utils;

import fr.Alphart.bat.i18n.I18n;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

public class EnhancedDateFormat{
	private final Calendar currDate = Calendar.getInstance();
	private final boolean litteralDate;
	private final DateFormat defaultDF;
	private DateFormat tdaDF;
	private DateFormat tmwDF;
	private DateFormat ydaDF;
	
	/**
	 * @param litteralDate if it's true, use tda, tmw or yda instead of the defautl date format
	 */
	public EnhancedDateFormat(final boolean litteralDate){
		this.litteralDate = litteralDate;
		final String at = I18n.format("at");
		defaultDF = new SimpleDateFormat("dd-MM-yyyy '" + at + "' HH:mm z");
		if(litteralDate){
			tdaDF = new SimpleDateFormat("'" + I18n.format("today").replace("'", "''") + " " + at + "' HH:mm z");
			tmwDF = new SimpleDateFormat("'" + I18n.format("tomorrow").replace("'", "''") + " " + at + "' HH:mm z");
			ydaDF = new SimpleDateFormat("'" + I18n.format("yesterday").replace("'", "''") + " " + at + "' HH:mm z");
		}
	}
	
	public String format(final Date date){
		if(litteralDate){
			final Calendar calDate = Calendar.getInstance();
			calDate.setTime(date);
			final int dateDoY = calDate.get(Calendar.DAY_OF_YEAR);
			final int currDoY = currDate.get(Calendar.DAY_OF_YEAR);
			
			if(calDate.get(Calendar.YEAR) == currDate.get(Calendar.YEAR)){
				if(dateDoY == currDoY){
					return tdaDF.format(date);
				}else if(dateDoY == currDoY - 1){
					return ydaDF.format(date);
				}else if(dateDoY == currDoY + 1){
					return tmwDF.format(date);
				}
			}
		}

		return defaultDF.format(date);
	}
}