package jaxb2.plugin.fields;

import com.sun.codemodel.JBlock;
import com.sun.codemodel.JClass;
import com.sun.codemodel.JCodeModel;
import com.sun.codemodel.JFieldVar;
import com.sun.codemodel.JMethod;
import com.sun.codemodel.JMod;
import com.sun.tools.xjc.Options;
import com.sun.tools.xjc.model.CPluginCustomization;
import com.sun.tools.xjc.outline.ClassOutline;
import com.sun.tools.xjc.outline.FieldOutline;
import com.sun.tools.xjc.outline.Outline;

import org.jvnet.jaxb2_commons.plugin.AbstractParameterizablePlugin;
import org.jvnet.jaxb2_commons.util.CustomizationUtils;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import javax.xml.namespace.QName;

public class ListSimpleGettersPlugin extends AbstractParameterizablePlugin {

    private static final String OPTION_NAME = "Xlist-simple-getters";

    private static final String NAMESPACE_URI = "http://list.simple.getters";

    /**
     * Tags that will be processed during plugin execution
     */
    public static final QName ATTR_CREATE_SIMPLE_GETTER_QNAME = new QName(NAMESPACE_URI, "create-simple-getter");

    @Override
    public String getOptionName() {
        return OPTION_NAME;
    }

    @Override
    public String getUsage() {
        return "-" + OPTION_NAME + ": Generate simple getters for List<?> fields";
    }

    /**
     * Plugin entry-point
     */
    @Override
    public boolean run(Outline outline, Options opt, ErrorHandler errorHandler)
            throws SAXException {

        for (ClassOutline classOutline : outline.getClasses()) {
            JCodeModel codeModel = classOutline.implClass.owner();
            JClass refList = codeModel.ref(List.class);

            Arrays.stream(classOutline.getDeclaredFields())
                    .filter(fieldOutline -> fieldOutline.getRawType().erasure() instanceof JClass)
                    .filter(fieldOutline -> refList.isAssignableFrom((JClass) fieldOutline.getRawType().erasure()))
                    .forEach(fieldOutline ->
                            getNamespaceCustomizations(fieldOutline)
                                    .stream()
                                    .filter(customization -> ATTR_CREATE_SIMPLE_GETTER_QNAME.getLocalPart().equals(customization.element.getLocalName()))
                                    .forEach(customization -> {

                                        //Mark that this customization was processed
                                        customization.markAsAcknowledged();

                                        JFieldVar jFieldVar = classOutline.implClass.fields().get(fieldOutline.getPropertyInfo().getName(false));

                                        String getterName = createConventionGetterName(jFieldVar.name());

                                        classOutline.implClass.methods()
                                                .removeIf(jMethod -> getterName.equals(jMethod.name()) && jMethod.params().isEmpty());

                                        createSimpleGetter(classOutline, jFieldVar, getterName);

                                    })
                    );
        }
        return true;
    }

    private String createConventionGetterName(String fieldName) {
        char firstLetter = fieldName.charAt(0);
        String firstLetterUpperCase = Character.valueOf(firstLetter).toString().toUpperCase();
        return "get" + firstLetterUpperCase + fieldName.substring(1);
    }

    private JMethod createSimpleGetter(ClassOutline classOutline, JFieldVar jFieldVar, String mehtodName) {
        JMethod jMethod = classOutline.implClass.method(JMod.PUBLIC, jFieldVar.type(), mehtodName);

        // Add java doc for method
        jMethod.javadoc().add("Gets the value of the " + jFieldVar.name() + " property.");

        // Add method body
        JBlock jBlock = jMethod.body();

        // return variable
        jBlock._return(jFieldVar);

        return jMethod;
    }

    /**
     * Return all customizations from NAMESPACE_URI namespace
     */
    private List<CPluginCustomization> getNamespaceCustomizations(FieldOutline fieldOutline) {
        return CustomizationUtils.getCustomizations(fieldOutline).stream()
                .filter(cPluginCustomization ->
                        NAMESPACE_URI.equals(cPluginCustomization.element.getNamespaceURI()))
                .collect(Collectors.toList());
    }

    /**
     * Returns list of QNAMEs which we know how to handle
     **/
    @Override
    public Collection<QName> getCustomizationElementNames() {
        return Arrays.asList(ATTR_CREATE_SIMPLE_GETTER_QNAME);
    }

}
