package me.starmism.batr.utils;

import me.starmism.batr.BATR;
import me.starmism.batr.i18n.I18n;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

public class EnhancedDateFormat {
    private final I18n i18n;
    private final Calendar currDate = Calendar.getInstance();
    private final boolean literalDate;
    private final DateFormat defaultDF;
    private DateFormat tdaDF;
    private DateFormat tmwDF;
    private DateFormat ydaDF;

    /**
     * @param literalDate if it's true, use tda, tmw or yda instead of the default date format
     */
    public EnhancedDateFormat(final boolean literalDate) {
        i18n = BATR.getInstance().getI18n();
        this.literalDate = literalDate;
        final String at = i18n.format("at");
        defaultDF = new SimpleDateFormat("dd-MM-yyyy '" + at + "' HH:mm z");
        if (literalDate) {
            tdaDF = new SimpleDateFormat("'" + i18n.format("today").replace("'", "''") + " " + at + "' HH:mm z");
            tmwDF = new SimpleDateFormat("'" + i18n.format("tomorrow").replace("'", "''") + " " + at + "' HH:mm z");
            ydaDF = new SimpleDateFormat("'" + i18n.format("yesterday").replace("'", "''") + " " + at + "' HH:mm z");
        }
    }

    public String format(final Date date) {
        if (literalDate) {
            final Calendar calDate = Calendar.getInstance();
            calDate.setTime(date);
            final int dateDoY = calDate.get(Calendar.DAY_OF_YEAR);
            final int currDoY = currDate.get(Calendar.DAY_OF_YEAR);

            if (calDate.get(Calendar.YEAR) == currDate.get(Calendar.YEAR)) {
                if (dateDoY == currDoY) {
                    return tdaDF.format(date);
                } else if (dateDoY == currDoY - 1) {
                    return ydaDF.format(date);
                } else if (dateDoY == currDoY + 1) {
                    return tmwDF.format(date);
                }
            }
        }
        return defaultDF.format(date);
    }
}