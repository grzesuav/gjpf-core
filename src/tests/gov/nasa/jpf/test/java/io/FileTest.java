/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package gov.nasa.jpf.test.java.io;

import gov.nasa.jpf.util.FileUtils;
import gov.nasa.jpf.util.test.TestJPF;
import java.io.File;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author proger
 */
public class FileTest extends TestJPF {

  public FileTest() {
  }

  @BeforeClass
  public static void setUpClass() throws Exception {
    // Creating sandbox for java.io.File testing
    File subdirs = new File("parent/child/child2");
    if (!subdirs.mkdirs())
      throw new RuntimeException("Unable to create sandbox directories");
  }

  @AfterClass
  public static void tearDownClass() throws Exception {
    if (!FileUtils.removeRecursively(new File("parent")))
      throw new RuntimeException("Unable to remove sandbox directories");
  }

  @Before
  public void setUp() {
  }

  @After
  public void tearDown() {
  }

  @Test
  public void testGetParentFile() {
    if (verifyNoPropertyViolation()) {
      File file = new File("parent/child");
      File expectedParent = new File("parent");
      File resultParent = file.getParentFile();
      
      assert expectedParent.equals(resultParent) == true;
    }
  }

  @Test
  public void testEquals() {
    if (verifyNoPropertyViolation()) {
      File file = new File("parent");
      File sameFile = new File("parent");
      File otherFile = new File("parent/child");

      assert file.equals(file) == true;
      assert file.equals(sameFile) == true;
      assert file.equals(otherFile) == false;
      assert file.equals(null) == false;
      assert file.equals(new Object()) == false;
    }
  }
  
}
