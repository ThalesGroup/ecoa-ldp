/* Copyright (c) 2025 THALES -- All rights reserved */

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.Locale;

import org.stringtemplate.v4.Interpreter;
import org.stringtemplate.v4.ST;
import org.stringtemplate.v4.STGroup;
import org.stringtemplate.v4.StringRenderer;
import org.stringtemplate.v4.misc.ObjectModelAdaptor;
import org.stringtemplate.v4.misc.STNoSuchPropertyException;

import com.thalesgroup.softarc.tools.AbstractGenerator;

/**
 * L'exécution de cette classe (sans paramètres) permet de générer tout le code Java du Formalisme, à partir de la définition du
 * Formalisme (elle aussi en Java), qui se trouve dans la classe SoftarcFormalismDefinition.
 */
public class Generate extends AbstractGenerator {

    public Generate() {
        super("GenFormalism", "templates");
    }

    @Override
    public void generate() throws Exception {
        final String userDir = System.getProperty("user.dir");
        File root = null;
        if (userDir != null)
            root = new File(userDir);
        File srcDestinationDir = new File(root, "src-gen");
        for (Class<?> c : SoftarcFormalismDefinition.class.getDeclaredClasses()) {
            checkClass(c);
            generate(c, "JavaInterface",
                    new File(srcDestinationDir, "com/thalesgroup/softarc/sf/" + c.getSimpleName() + ".java"));
            generate(c, "JavaClass",
                    new File(srcDestinationDir, "com/thalesgroup/softarc/sf/impl/Q" + c.getSimpleName() + ".java"));
        }
        createFileFromTemplate(new File(root, "formalism/attributes.properties"),
                "genListAttributes", "genListAttributes", "SoftarcFormalismDefinition", SoftarcFormalismDefinition.class);
    }

    private void checkClass(Class<?> c) {
        for (Field f : c.getDeclaredFields())
            if (!f.isSynthetic() && !isTypeAllowedInFormalism(f.getType()))
                errorModel("Type %s not allowed for field %s.%s", f.getType().toString(), c.getSimpleName(), f.getName());
    }

    private boolean isTypeAllowedInFormalism(Class<?> t) {
        if (t.isArray()) {
            return isTypeAllowedInFormalismArray(t.getComponentType());
        } else if (t == Boolean.TYPE) {
            return true;
        } else if (t == Long.TYPE) {
            return true;
        } else if (t == String.class) {
            return true;
        } else if (t == Long.class) {
            return true;
        } else if (t.getEnclosingClass() == SoftarcFormalismDefinition.class) {
            return true;
        } else
            return false;
    }

    private boolean isTypeAllowedInFormalismArray(Class<?> t) {
        if (t == String.class) {
            return true;
        } else if (t == Long.class) {
            return true;
        } else if (t.getEnclosingClass() == SoftarcFormalismDefinition.class) {
            return true;
        } else
            return false;
    }

    @Override
    protected void initialize() throws Exception {
    }

    public void registerRenderers(STGroup group) {
        group.registerRenderer(String.class, new StringRenderer() {
            @Override
            public String toString(Object o, String formatString, Locale locale) {
                if (formatString != null && formatString.equals("firstToUpperCase")) {
                    String s = (String) o;
                    if (!s.isEmpty()) {
                        char first = s.charAt(0);
                        if (first == '$') {
                            s = s.substring(1);
                            first = s.charAt(0);
                        }
                        s = Character.toUpperCase(first) + s.substring(1);
                    }
                    return s;
                } else {
                    return super.toString(o, formatString, locale);
                }
            }
        });
        // group.registerModelAdaptor(Class.class, new ObjectModelAdaptor() {
        // @Override
        // public Object getProperty(Interpreter interp, ST self, Object o, Object property, String propertyName)
        // throws STNoSuchPropertyException {
        // if (propertyName.equals("isString"))
        // return o instanceof String;
        // return super.getProperty(interp, self, o, property, propertyName);
        // }
        // });
        group.registerModelAdaptor(Field.class, new ObjectModelAdaptor() {
            @Override
            public Object getProperty(Interpreter interp, ST self, Object o, Object property, String propertyName)
                    throws STNoSuchPropertyException {
                if (propertyName.equals("contains"))
                    return o instanceof Field && ((Field) o).getDeclaredAnnotation(contains.class) != null;
                return super.getProperty(interp, self, o, property, propertyName);
            }
        });
    }

    void generate(Class<?> c, String templateName, File outputFile) throws IOException {
        // info("%s ==> %s", c.getSimpleName(), outputFile.getAbsolutePath());
        // info("type=%s", c.getDeclaredFields()[0].getType().getComponentType().getName());
        createFileFromTemplate(outputFile, "java", templateName, "c", c);
    }

    static public void main(String[] args) throws Exception {
        Generate g = new Generate();
        g.execute(new String[] {});
    }
}
