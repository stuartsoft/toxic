package toxic.dsl

import groovy.mock.interceptor.MockFor
import org.apache.commons.compress.archivers.tar.TarArchiveEntry
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream
import org.apache.commons.compress.compressors.gzip.GzipCompressorOutputStream
import org.apache.commons.io.IOUtils
import org.junit.Test
import toxic.ToxicProperties

import static org.junit.Assert.fail

class DepResolverTest {
  @Test
  void should_construct() {
    def props = ['pickle.repoUrl': 'http://localhost', homePath: '/tmp', 'pickle.ext': 'tgz']

    DepResolver depResolver = new DepResolver('foo', props)
    assert 'http://localhost/foo.tgz' == depResolver.url
    assert '/tmp/gen/deps/foo' == depResolver.depsDir.absolutePath

    depResolver = new DepResolver('foo.tgz', props)
    assert 'http://localhost/foo.tgz' == depResolver.url
    assert '/tmp/gen/deps/foo' == depResolver.depsDir.absolutePath

    depResolver = new DepResolver('foo-1.0.0', props)
    assert 'http://localhost/foo-1.0.0.tgz' == depResolver.url
    assert '/tmp/gen/deps/foo-1.0.0' == depResolver.depsDir.absolutePath

    depResolver = new DepResolver('foo-1.0.0.tgz', props)
    assert 'http://localhost/foo-1.0.0.tgz' == depResolver.url
    assert '/tmp/gen/deps/foo-1.0.0' == depResolver.depsDir.absolutePath

    depResolver = new DepResolver('foo-1.0.0.zip', props)
    assert 'http://localhost/foo-1.0.0.zip.tgz' == depResolver.url
    assert '/tmp/gen/deps/foo-1.0.0.zip' == depResolver.depsDir.absolutePath
  }

  @Test
  void should_construct_without_username_and_password() {
    ToxicProperties toxicProperties = new ToxicProperties()
    toxicProperties['pickle.repoUrl'] = 'http://localhost'
    toxicProperties['pickle.ext'] = 'tgz'
    toxicProperties['homePath'] = '/tmp'
    DepResolver depResolver = new DepResolver('foo', toxicProperties)
    assert null == depResolver.username
    assert null == depResolver.password
  }

  @Test
  void should_fail_when_base_url_is_not_configured() {
    try {
      new DepResolver('foo', [:])
      fail('Expected exception')
    }
    catch(DependencyResolutionException e) {
      assert 'Missing required property -pickle.repoUrl' == e.message
    }
  }

  @Test
  void should_resolve_from_secure_repo() {
    withResolver { DepResolver depResolver ->
      withTarGz { File file ->
        withFileUrl(file) { def requestProps ->
          depResolver.resolve()
          assert ['Authorization':'Basic Zm9vOmJhcg=='] == requestProps
        }
      }
    }
  }

  @Test
  void should_resolve_from_unsecure_repo() {
    withResolver { DepResolver depResolver ->
      withTarGz { File file ->
        withFileUrl(file) { def requestProps ->
          depResolver.username = null
          depResolver.password = null
          depResolver.resolve()
          assert [:] == requestProps
        }
      }
    }
  }

  @Test
  void should_resolve_tgz() {
    withResolver { DepResolver depResolver ->
      withTarGz { File file ->
        withFileUrl(file) { def requestProps ->
          depResolver.resolve()
          assert 2 == depResolver.depsDir.listFiles().size()

          File fnDir = new File(depResolver.depsDir, 'functions')
          assert 1 == fnDir.listFiles().size()
          assert 'function test{}' == new File(fnDir, 'test.fn').text

          File libDir = new File(depResolver.depsDir, 'library')
          assert 1 == libDir.listFiles().size()
          assert 'assert 1==1' == new File(libDir, 'test.groovy').text
        }
      }
    }
  }

  @Test
  void should_remove_existing_dep_dir() {
    withResolver { DepResolver depResolver ->
      withTarGz { File file ->
        withFileUrl(file) { def requestProps ->
          new File(depResolver.depsDir, 'previousFnDir').mkdirs()
          depResolver.resolve()
          assert 2 == depResolver.depsDir.listFiles().size()
          assert new File(depResolver.depsDir, 'functions').exists()
          assert new File(depResolver.depsDir, 'library').exists()
        }
      }
    }
  }

  void withResolver(Closure c) {
    File tempDir
    try {
      tempDir = File.createTempDir()
      def props = ['pickle.repoUrl':'http://localhost', 'pickle.repoUsername': 'foo', 'pickle.repoPassword': 'bar', homePath: tempDir]
      DepResolver depResolver = new DepResolver('foobar.tgz', props)
      c(depResolver)
    }
    finally {
      tempDir?.deleteDir()
    }
  }

  void withTarGz(Closure c) {
    File file
    File tempDir
    try {
      file = File.createTempFile('dep_resolver', '.tgz')
      tempDir = File.createTempDir()
      File fnDir = new File(tempDir, 'functions')
      File libDir = new File(tempDir, 'library')

      fnDir.mkdirs()
      libDir.mkdirs()

      new File(fnDir, 'test.fn').text = 'function test{}'
      new File(libDir, 'test.groovy').text = 'assert 1==1'

      tarGz(file, fnDir, libDir)
      c(file)
    }
    finally {
      file?.delete()
      tempDir?.deleteDir()
    }
  }

  void withFileUrl(File file, Closure c) {
    MockFor urlMock = new MockFor(URL)
    String url = "http://localhost/foo${DepResolver.fileExtension(file)}"
    def requestProps = [:]
    def mockUrlConnection = new URLConnection(new URL(url)){
      void connect() throws IOException { }
      InputStream getInputStream() throws IOException {
        file.newInputStream()
      }
      void setRequestProperty(String key, String value) {
        requestProps[key] = value
      }
    }
    urlMock.demand.openConnection(1) {
      mockUrlConnection
    }
    urlMock.demand.getPath(1) { url }
    urlMock.use {
      c(requestProps)
    }
  }

  void tarGz(File file, File... dirs) {
    TarArchiveOutputStream outputStream = new TarArchiveOutputStream(new GzipCompressorOutputStream(new BufferedOutputStream(new FileOutputStream(file))))
    dirs.each {
      addFileToTarGz(outputStream, it)
    }
    outputStream.close()
  }

  void addFileToTarGz(TarArchiveOutputStream outputStream, File file, String base = '') {
    String entryName = "${base}${file.getName()}"
    TarArchiveEntry tarEntry = new TarArchiveEntry(file, entryName)
    outputStream.putArchiveEntry(tarEntry)

    if (file.isFile()) {
      IOUtils.copy(new FileInputStream(file), outputStream)
      outputStream.closeArchiveEntry()
    }
    else {
      outputStream.closeArchiveEntry()
      file.eachFile {
        addFileToTarGz(outputStream, it, "${entryName}/")
      }
    }
  }
}
