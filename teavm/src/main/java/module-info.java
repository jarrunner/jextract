/** The TeaVM binding generator: TeaVM @Import bindings from real C headers, using jextract's parser. */
module jarrunner.jextract_teavm {
    requires org.openjdk.jextract;
    requires info.picocli;
    opens jarrunner.jextract_teavm to info.picocli;
}
