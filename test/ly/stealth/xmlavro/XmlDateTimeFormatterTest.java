package ly.stealth.xmlavro;

import org.joda.time.DateTimeZone;
import org.junit.Test;
import static org.junit.Assert.*;

public class XmlDateTimeFormatterTest {

  @Test
  public void shouldFormatXmlBasedOnSolarTimeUsingDefaultUTCTime() {
    String xmlDateNoTz = "2014-10-09T12:58:33";
    long epochTs = new XmlDateTimeFormatter().parseMillis(xmlDateNoTz);
    assertEquals(1412859513000L, epochTs);
  }

  @Test
  public void shouldFormatXmlWithDecimalsBasedOnSolarTimeUsingDefaultUTCTime() {
    String xmlDateWithDecimalNoTz = "2014-10-09T12:58:33.5";
    long epochTs = new XmlDateTimeFormatter().parseMillis(xmlDateWithDecimalNoTz);
    assertEquals(1412859513500L, epochTs);
  }

  @Test
  public void shouldFormatXmlDateWithDaylightSavingUsingBSTTimeZone() {
    String xmlDateNoTz = "2014-10-09T12:58:33";
    long epochTs = new XmlDateTimeFormatter(DateTimeZone.forID("Europe/London")).parseMillis(xmlDateNoTz);
    assertEquals(1412855913000L, epochTs);
  }

  @Test
  public void shouldFormatXmlDateWithUtcTZWithDefaultBSTTimeZone() {
    String xmlDateNoTz = "2014-10-09T12:58:33Z";
    long epochTs = new XmlDateTimeFormatter(DateTimeZone.forID("Europe/London")).parseMillis(xmlDateNoTz);
    assertEquals(1412859513000L, epochTs);
  }

  @Test
  public void shouldFormatXmlDateWithCetTZ() {
    String xmlDateWithCetTz = "2014-10-09T14:58:33+02:00";
    long epochTs = new XmlDateTimeFormatter().parseMillis(xmlDateWithCetTz);
    assertEquals(1412859513000L, epochTs);
  }
}
