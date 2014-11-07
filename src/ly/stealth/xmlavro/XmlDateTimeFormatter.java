package ly.stealth.xmlavro;

import org.joda.time.DateTimeZone;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import java.util.regex.Pattern;

class XmlDateTimeFormatter {
  private final DateTimeFormatter dateTimeFormatter;
  private final DateTimeFormatter dateTimeFormatterTz;
  private final DateTimeFormatter dateTimeFormatterWithDecimals;
  private final DateTimeFormatter dateTimeFormatterWithDecimalsTz;
  private final Pattern timeZonePattern = Pattern.compile(".*[+-][0-9][0-9]:[0-9][0-9]");

  XmlDateTimeFormatter() {
    this(DateTimeZone.UTC);
  }

  XmlDateTimeFormatter(DateTimeZone zone) {
    dateTimeFormatter = DateTimeFormat.forPattern("yyyy-MM-dd'T'HH:mm:ss").withZone(zone);
    dateTimeFormatterTz = DateTimeFormat.forPattern("yyyy-MM-dd'T'HH:mm:ssZ");
    dateTimeFormatterWithDecimals = DateTimeFormat.forPattern("yyyy-MM-dd'T'HH:mm:ss.S").withZone(zone);
    dateTimeFormatterWithDecimalsTz = DateTimeFormat.forPattern("yyyy-MM-dd'T'HH:mm:ss.SZ");
  }

  boolean isDateTimeXmlValue(String dateText) {
    return dateText.indexOf('T') > 0;
  }

  DateTimeFormatter getDateTimeFormat(String dateText) {
    if (hasDecimal(dateText)) {
      return endsWithTimeZone(dateText) ? dateTimeFormatterWithDecimalsTz : dateTimeFormatterWithDecimals;
    } else {
      return endsWithTimeZone(dateText) ? dateTimeFormatterTz : dateTimeFormatter;
    }
  }

  private boolean hasDecimal(String dateText) {
    return dateText.indexOf('.') > 0;
  }

  private boolean endsWithTimeZone(String dateText) {
    boolean matches = timeZonePattern.matcher(dateText).matches();
    return matches;
  }

  long parseMillis(String text) {
    DateTimeFormatter formatter = getDateTimeFormat(text);
    if (text.endsWith("Z")) {
      return formatter.withZoneUTC().parseMillis(text.substring(0, text.length() - 1));
    } else {
      return formatter.parseMillis(text);
    }
  }
}
