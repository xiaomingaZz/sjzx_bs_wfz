package tdh.util;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Properties;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * http://blog.csdn.net/mylove2008cq/article/details/3930576
 * 用来加载类, classpath下的资源文件，属性文件等。
 * getExtendResource(StringrelativePath)方法，可以使用../符号来加载classpath外部的资源。
 */
public class ClassLoaderUtil {
  private static Log log = LogFactory.getLog(ClassLoaderUtil.class);

  /**
   * 加载Java类。 使用全限定类名
   * 
   * @paramclassName
   * @return
   */
  @SuppressWarnings("unchecked")
  public static Class loadClass(String className) {
    try {
      return getClassLoader().loadClass(className);
    } catch (ClassNotFoundException e) {
      throw new RuntimeException("class not found ’" + className + "’", e);
    }
  }

  /**
   * 得到类加载器
   * 
   * @return
   */
  public static ClassLoader getClassLoader() {
    return ClassLoaderUtil.class.getClassLoader();
  }

  /**
   * 提供相对于classpath的资源路径，返回文件的输入流
   * 
   * @param relativePath必须传递资源的相对路径。是相对于classpath的路径。如果需要查找classpath外部的资源，需要使用../来查找
   * @return 文件输入流
   * @throws IOException
   * @throws MalformedURLException
   */

  public static InputStream getStream(String relativePath) throws MalformedURLException, IOException {
    if (!relativePath.contains("../")) {
      return getClassLoader().getResourceAsStream(relativePath);
    } else {
      return ClassLoaderUtil.getStreamByExtendResource(relativePath);
    }
  }

  /**
   * 
   * @param url
   * @return
   * @throwsIOException
   */
  public static InputStream getStream(URL url) throws IOException {
    if (url != null) {
      return url.openStream();
    } else {
      return null;
    }
  }

  /**
   * 
   * @param relativePath
   *          必须传递资源的相对路径。是相对于classpath的路径。如果需要查找classpath外部的资源，需要使用../来查找
   * @return
   * @throws MalformedURLException
   * @throws IOException
   */
  public static InputStream getStreamByExtendResource(String relativePath) throws MalformedURLException, IOException {
    return ClassLoaderUtil.getStream(ClassLoaderUtil.getExtendResource(relativePath));
  }

  /**
   * 提供相对于classpath的资源路径，返回属性对象，它是一个散列表
   * 
   * @return
   */
  public static Properties getProperties(String resource) {
    Properties properties = new Properties();
    try {
      properties.load(getStream(resource));
    } catch (IOException e) {
      throw new RuntimeException("couldn’t load properties file ’" + resource + "’", e);
    }
    return properties;
  }

  /**
   * 得到本Class所在的ClassLoader的绝对路径。 URL形式的
   * 
   * @return
   */
  public static String getAbsolutePathOfClassLoaderClassPath() {
    ClassLoaderUtil.log.debug(ClassLoaderUtil.getClassLoader().getResource("").toString());
    return ClassLoaderUtil.getClassLoader().getResource("").toString();
  }

  /**
   * 
   * @param relativePath 相对于classpath的路径
   * @return 资源的绝对URL
   * @throws MalformedURLException
   */
  public static URL getExtendResource(String relativePath) throws MalformedURLException {
    ClassLoaderUtil.log.debug("传入的相对路径：" + relativePath);
    if (!relativePath.contains("../")) {
      return ClassLoaderUtil.getResource(relativePath);
    }
    String classPathAbsolutePath = ClassLoaderUtil.getAbsolutePathOfClassLoaderClassPath();
    if (relativePath.substring(0, 1).equals("/")) {
      relativePath = relativePath.substring(1);
    }

    String wildcardString = relativePath.substring(0, relativePath.lastIndexOf("../") + 3);
    relativePath = relativePath.substring(relativePath.lastIndexOf("../") + 3);
    int containSum = ClassLoaderUtil.containSum(wildcardString, "../");

    classPathAbsolutePath = ClassLoaderUtil.cutLastString(classPathAbsolutePath, "/", containSum);
    String resourceAbsolutePath = classPathAbsolutePath + relativePath;
    ClassLoaderUtil.log.debug("绝对路径：" + resourceAbsolutePath);
    URL resourceAbsoluteURL = new URL(resourceAbsolutePath);
    return resourceAbsoluteURL;
  }

  private static int containSum(String source, String dest) {
    int containSum = 0;
    int destLength = dest.length();
    while (source.contains(dest)) {
      containSum = containSum + 1;
      source = source.substring(destLength);
    }
    return containSum;
  }

  private static String cutLastString(String source, String dest, int num) {
    for (int i = 0; i < num; i++) {
      source = source.substring(0, source.lastIndexOf(dest, source.length() - 2) + 1);
    }
    return source;
  }

  public static URL getResource(String resource) {
    ClassLoaderUtil.log.debug("传入的相对于classpath的路径：" + resource);
    return ClassLoaderUtil.getClassLoader().getResource(resource);
  }

  public static void main(String[] args) throws MalformedURLException {
    // 相对路径
    System.out.println(ClassLoaderUtil.getExtendResource("../config/log4j.properties").toString());
    System.out.println(ClassLoaderUtil.getExtendResource("tdh/test/log4j.properties").toString());
    
    // classes主目录下
    System.out.println(ClassLoaderUtil.getClassLoader().getResource("log4j.properties").toString());
  }

}