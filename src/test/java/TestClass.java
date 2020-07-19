import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class TestClass {

  @Test
  public void test1() {

    MainClass mainClass = new MainClass();

    assertFalse(mainClass.isCorrectDate("12313", MainClass.formatter));

    assertTrue(mainClass.isCorrectDate("31.12.2003", MainClass.formatter));

    assertFalse(mainClass.isStringDouble("3.0 asda"));

    assertTrue(mainClass.isStringDouble("22 000,00"));
    assertTrue(mainClass.isStringDouble("-22 000,00"));
    assertTrue(mainClass.isStringDouble("20,00"));
    assertTrue(mainClass.isStringDouble("+20,00"));
    //assertTrue( mainClass.isStringDouble("+22 000,00"));

    assertEquals(mainClass.getDoubleFromString("13 759,61"), -13759.61f, 0.01);
    assertEquals(mainClass.getDoubleFromString("+13 759,61"), 13759.61f, 0.01);
  }

}
