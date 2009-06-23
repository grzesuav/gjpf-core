
package gov.nasa.jpf.util.test;

/**
 * an exception in the test framework
 */
public class TestException extends RuntimeException {

  public TestException(String details) {
    super(details);
  }

  public TestException(String details, Throwable cause) {
    super(details, cause);
  }

}
