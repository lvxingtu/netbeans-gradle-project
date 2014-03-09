package org.netbeans.gradle.project.properties2;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.jtrim.cancel.Cancellation;
import org.jtrim.cancel.OperationCanceledException;
import org.jtrim.concurrent.WaitableSignal;
import org.jtrim.property.MutableProperty;
import org.jtrim.property.PropertyFactory;
import org.jtrim.property.PropertySource;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.*;

public class ProfileSettingsTest {
    @BeforeClass
    public static void setUpClass() {
    }

    @AfterClass
    public static void tearDownClass() {
    }

    @Before
    public void setUp() {
    }

    @After
    public void tearDown() {
    }

    private static String getResourcePath(String relPath) {
        return ProfileSettingsTest.class.getPackage().getName().replace('.', '/') + "/" + relPath;
    }

    private static InputStream openResource(String relPath) throws IOException {
        String absolutePath = getResourcePath(relPath);
        ClassLoader classLoader = ProfileSettingsTest.class.getClassLoader();

        URL url = classLoader.getResource(absolutePath);
        if (url == null) {
            throw new IOException("No URL for resource: " + absolutePath);
        }

        InputStream result = classLoader.getResourceAsStream(absolutePath);
        if (result == null) {
            throw new IOException("Failed to open resource: " + absolutePath);
        }
        return result;
    }

    private static void readDocument(ProfileSettings settings, String configFileName) throws IOException {
        try (InputStream input = openResource(configFileName)) {
            settings.loadFromStream(input);
        }
    }

    private static void readFromSettings1(ProfileSettings settings) throws IOException {
        readDocument(settings, "settings1.xml");
    }

    private static ConfigPath getConfigPath(String... keys) {
        List<ConfigKey> configKeys = new ArrayList<>(keys.length);
        for (String key: keys) {
            configKeys.add(new ConfigKey(key, null));
        }
        return ConfigPath.fromKeys(configKeys);
    }

    private static <ValueKey, ValueType> MutableProperty<ValueType> getProperty(
            ProfileSettings settings,
            PropertyDef<ValueKey, ValueType> propertyDef,
            String... keys) {

        ConfigPath configPath = getConfigPath(keys);
        return settings.getProperty(configPath, propertyDef);
    }

    private static void setSimpleEncodingDef(PropertyDef.Builder<ConfigTree, ?> result) {
        result.setKeyEncodingDef(new PropertyKeyEncodingDef<ConfigTree>() {
            @Override
            public ConfigTree decode(ConfigTree config) {
                return config;
            }

            @Override
            public ConfigTree encode(ConfigTree value) {
                return value;
            }
        });
    }

    private static PropertyDef<ConfigTree, ConfigTree> getNodeProfileDef() {
        PropertyDef.Builder<ConfigTree, ConfigTree> result = new PropertyDef.Builder<>();
        result.setValueDef(new PropertyValueDef<ConfigTree, ConfigTree>() {
            @Override
            public PropertySource<ConfigTree> property(ConfigTree valueKey) {
                return PropertyFactory.constSource(valueKey);
            }

            @Override
            public ConfigTree getKeyFromValue(ConfigTree value) {
                return value;
            }
        });
        setSimpleEncodingDef(result);

        return result.create();
    }

    private static PropertyDef<PlatformId, PlatformId> getTargetPlatformProfileDef() {
        PropertyDef.Builder<PlatformId, PlatformId> result = new PropertyDef.Builder<>();
        result.setValueDef(new PropertyValueDef<PlatformId, PlatformId>() {
            @Override
            public PropertySource<PlatformId> property(PlatformId valueKey) {
                return PropertyFactory.constSource(valueKey);
            }

            @Override
            public PlatformId getKeyFromValue(PlatformId value) {
                return value;
            }
        });
        result.setKeyEncodingDef(StandardProperties.getTargetPlatformEncodingDef());

        return result.create();
    }

    private static PropertyDef<ConfigTree, String> getTextProfileDef() {
        PropertyDef.Builder<ConfigTree, String> result = new PropertyDef.Builder<>();
        result.setValueDef(new PropertyValueDef<ConfigTree, String>() {
            @Override
            public PropertySource<String> property(ConfigTree valueKey) {
                return PropertyFactory.constSource(valueKey != null
                        ? valueKey.getValue(null)
                        : null);
            }

            @Override
            public ConfigTree getKeyFromValue(String value) {
                if (value == null) {
                    return null;
                }

                return ConfigTree.singleValue(value);
            }
        });
        setSimpleEncodingDef(result);

        return result.create();
    }

    private static MutableProperty<String> getTextProperty(
            ProfileSettings settings,
            String... keys) {

        ConfigPath configPath = getConfigPath(keys);
        return settings.getProperty(configPath, getTextProfileDef());
    }

    private static MutableProperty<ConfigTree> getProperty(
            ProfileSettings settings,
            String... keys) {

        ConfigPath configPath = getConfigPath(keys);
        return settings.getProperty(configPath, getNodeProfileDef());
    }

    private static void assertTextNodePropertyValue(
            MutableProperty<ConfigTree> property,
            String expectedValue) {

        ConfigTree value = property.getValue();

        assertNotNull(property + " must have a value.", value);
        assertEquals(property.toString(), expectedValue, value.getValue(null));
    }

    private static void assertTextNodePropertyValue(
            ProfileSettings settings,
            String propertyName,
            String expectedValue) {

        MutableProperty<ConfigTree> property = getProperty(settings, propertyName);
        assertTextNodePropertyValue(property, expectedValue);
    }

    private void testSetValueOfTextProperty(String initialValue, String newValue, String... propertyPath) throws IOException {
        ProfileSettings settings = new ProfileSettings();
        readFromSettings1(settings);

        String propertyName = Arrays.toString(propertyPath);

        MutableProperty<String> property = getTextProperty(settings, propertyPath);
        assertEquals(propertyName, initialValue, property.getValue());

        WaitableListener documentListener = new WaitableListener();
        settings.addDocumentChangeListener(documentListener);

        WaitableListener listener = new WaitableListener();
        property.addChangeListener(listener);

        property.setValue(newValue);
        assertEquals(propertyName, newValue, property.getValue());

        listener.waitForCall("Value change for text node.");
        documentListener.waitForCall("Document change for text node.");
    }

    @Test
    public void testSetValueOfRootTextProperty() throws IOException {
        testSetValueOfTextProperty("UTF-8", "ISO-8859-1", "source-encoding");
        testSetValueOfTextProperty("j2se", "j2me", "target-platform-name");
        testSetValueOfTextProperty("1.7", "1.7", "target-platform");
        testSetValueOfTextProperty("1.7", "1.7", "source-level");
    }

    @Test
    public void testSetValueOfDeepTextProperty() throws IOException {
        testSetValueOfTextProperty("LF", "CRLF", "auxiliary", "com-junichi11-netbeans-changelf.lf-kind");
    }

    @Test
    public void testMultiNodeProperty() throws IOException {
        ProfileSettings settings = new ProfileSettings();
        readFromSettings1(settings);

        List<ConfigPath> configPaths = Arrays.asList(
                getConfigPath("target-platform-name"),
                getConfigPath("target-platform"));

        MutableProperty<PlatformId> property
                = settings.getProperty(configPaths, getTargetPlatformProfileDef());

        String propertyName = "TargetPlatform";

        assertEquals(propertyName, new PlatformId("j2se", "1.7"), property.getValue());

        WaitableListener documentListener = new WaitableListener();
        settings.addDocumentChangeListener(documentListener);

        WaitableListener listener = new WaitableListener();
        property.addChangeListener(listener);

        PlatformId newValue = new PlatformId("j2me", "1.5");
        property.setValue(newValue);
        assertEquals(propertyName, newValue, property.getValue());

        listener.waitForCall("Value change for multi node.");
        documentListener.waitForCall("Document change for multi node.");
    }

    private static final class WaitableListener implements Runnable {
        private final WaitableSignal calledSignal;

        public WaitableListener() {
            this.calledSignal = new WaitableSignal();
        }

        @Override
        public void run() {
            calledSignal.signal();
        }

        public void waitForCall(String taskName) {
            if (!calledSignal.tryWaitSignal(Cancellation.UNCANCELABLE_TOKEN, 10, TimeUnit.SECONDS)) {
                throw new OperationCanceledException("Timeout: " + taskName);
            }
        }
    }
}