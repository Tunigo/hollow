/*
 *
 *  Copyright 2016 Netflix, Inc.
 *
 *     Licensed under the Apache License, Version 2.0 (the "License");
 *     you may not use this file except in compliance with the License.
 *     You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 *     Unless required by applicable law or agreed to in writing, software
 *     distributed under the License is distributed on an "AS IS" BASIS,
 *     WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *     See the License for the specific language governing permissions and
 *     limitations under the License.
 *
 */
package com.netflix.hollow.api.codegen.objects;

import static com.netflix.hollow.api.codegen.HollowCodeGenerationUtils.delegateInterfaceName;
import static com.netflix.hollow.api.codegen.HollowCodeGenerationUtils.hollowImplClassname;
import static com.netflix.hollow.api.codegen.HollowCodeGenerationUtils.substituteInvalidChars;
import static com.netflix.hollow.api.codegen.HollowCodeGenerationUtils.typeAPIClassname;
import static com.netflix.hollow.api.codegen.HollowCodeGenerationUtils.uppercase;

import com.netflix.hollow.api.custom.HollowAPI;

import com.netflix.hollow.core.schema.HollowObjectSchema;
import com.netflix.hollow.api.codegen.HollowAPIGenerator;
import com.netflix.hollow.api.codegen.HollowJavaFileGenerator;
import com.netflix.hollow.api.objects.HollowObject;
import java.util.Set;

/**
 * This class contains template logic for generating a {@link HollowAPI} implementation.  Not intended for external consumption.
 * 
 * @see HollowAPIGenerator
 * 
 */
public class HollowObjectJavaGenerator implements HollowJavaFileGenerator {

    private final HollowObjectSchema schema;
    private final String packageName;
    private final String apiClassname;
    private final String className;
    private final Set<String> parameterizedTypes;
    private final boolean parameterizeClassNames;
    private final String classPostfix;
    private final String getterPrefix;

    public HollowObjectJavaGenerator(String packageName, String apiClassname, HollowObjectSchema schema, Set<String> parameterizedTypes, boolean parameterizeClassNames, String classPostfix, String getterPrefix) {
        this.packageName = packageName;
        this.apiClassname = apiClassname;
        this.schema = schema;
        this.className = hollowImplClassname(schema.getName(), classPostfix);
        this.parameterizedTypes = parameterizedTypes;
        this.parameterizeClassNames = parameterizeClassNames;
        this.classPostfix = classPostfix;
        this.getterPrefix = getterPrefix;
    }

    @Override
    public String getClassName() {
        return className;
    }

    @Override
    public String generate() {
        StringBuilder classBuilder = new StringBuilder();

        classBuilder.append("package " + packageName + ";\n\n");

        classBuilder.append("import " + HollowObject.class.getName() + ";\n");
        classBuilder.append("import " + HollowObjectSchema.class.getName() + ";\n\n");


        classBuilder.append("@SuppressWarnings(\"all\")\n");
        classBuilder.append("public class " + className + " extends HollowObject {\n\n");

        appendConstructor(classBuilder);

        appendAccessors(classBuilder);

        appendAPIAccessor(classBuilder);
        appendTypeAPIAccessor(classBuilder);
        appendDelegateAccessor(classBuilder);

        classBuilder.append("}");

        return classBuilder.toString();
    }

    private void appendConstructor(StringBuilder classBuilder) {
        classBuilder.append("    public " + className + "(" + delegateInterfaceName(schema.getName()) + " delegate, int ordinal) {\n");
        classBuilder.append("        super(delegate, ordinal);\n");
        classBuilder.append("    }\n\n");
    }

    private void appendAccessors(StringBuilder classBuilder) {
        for(int i=0;i<schema.numFields();i++) {
            switch(schema.getFieldType(i)) {
                case BOOLEAN:
                    classBuilder.append(generateBooleanFieldAccessor(i));
                    break;
                case BYTES:
                    classBuilder.append(generateByteArrayFieldAccessor(i));
                    break;
                case DOUBLE:
                    classBuilder.append(generateDoubleFieldAccessor(i));
                    break;
                case FLOAT:
                    classBuilder.append(generateFloatFieldAccessor(i));
                    break;
                case INT:
                    classBuilder.append(generateIntFieldAccessor(i));
                    break;
                case LONG:
                    classBuilder.append(generateLongFieldAccessor(i));
                    break;
                case REFERENCE:
                    classBuilder.append(generateReferenceFieldAccessor(i));
                    break;
                case STRING:
                    classBuilder.append(generateStringFieldAccessors(i));
                    break;
            }

            classBuilder.append("\n\n");
        }
    }

    private String generateByteArrayFieldAccessor(int fieldNum) {
        StringBuilder builder = new StringBuilder();

        String fieldName = substituteInvalidChars(schema.getFieldName(fieldNum));

        builder.append("    public byte[] ").append(getterPrefix).append("get" + uppercase(fieldName) + "() {\n");
        builder.append("        return delegate().get" + uppercase(fieldName) + "(ordinal);\n");
        builder.append("    }");

        return builder.toString();
    }

    private String generateStringFieldAccessors(int fieldNum) {
        StringBuilder builder = new StringBuilder();

        String fieldName = substituteInvalidChars(schema.getFieldName(fieldNum));

        builder.append("    public String ").append(getterPrefix).append("get" + uppercase(fieldName) + "() {\n");
        builder.append("        return delegate().get" + uppercase(fieldName) + "(ordinal);\n");
        builder.append("    }\n\n");

        builder.append("    public boolean ").append(getterPrefix).append("is" + uppercase(fieldName) + "Equal(String testValue) {\n");
        builder.append("        return delegate().is" + uppercase(fieldName) + "Equal(ordinal, testValue);\n");
        builder.append("    }");

        return builder.toString();
    }

    private String generateReferenceFieldAccessor(int fieldNum) {
        StringBuilder builder = new StringBuilder();

        String fieldName = substituteInvalidChars(schema.getFieldName(fieldNum));
        String referencedType = schema.getReferencedType(fieldNum);
        
        boolean parameterize = parameterizeClassNames || parameterizedTypes.contains(referencedType);

        if(parameterize)
            builder.append("    public <T> T ").append(getterPrefix).append("get"+ uppercase(fieldName) + "() {\n");
        else
            builder.append("    public ").append(hollowImplClassname(referencedType, classPostfix)).append(" ").append(getterPrefix).append("get"+ uppercase(fieldName) + "() {\n");

        builder.append("        int refOrdinal = delegate().get" + uppercase(fieldName) + "Ordinal(ordinal);\n");
        builder.append("        if(refOrdinal == -1)\n");
        builder.append("            return null;\n");
        builder.append("        return ").append(parameterize ? "(T)" : "").append(" api().get" + hollowImplClassname(referencedType, classPostfix) + "(refOrdinal);\n");
        builder.append("    }");

        return builder.toString();
    }

    private String generateFloatFieldAccessor(int fieldNum) {
        StringBuilder builder = new StringBuilder();

        String fieldName = substituteInvalidChars(schema.getFieldName(fieldNum));

        builder.append("    public float ").append(getterPrefix).append("get").append(uppercase(fieldName)).append("() {\n");
        builder.append("        return delegate().get" + uppercase(fieldName) + "(ordinal);\n");
        builder.append("    }\n\n");

        builder.append("    public Float ").append(getterPrefix).append("get").append(uppercase(fieldName)).append("Boxed() {\n");
        builder.append("        return delegate().get" + uppercase(fieldName) + "Boxed(ordinal);\n");
        builder.append("    }");


        return builder.toString();
    }

    private String generateDoubleFieldAccessor(int fieldNum) {
        StringBuilder builder = new StringBuilder();

        String fieldName = substituteInvalidChars(schema.getFieldName(fieldNum));

        builder.append("    public double ").append(getterPrefix).append("get").append(uppercase(fieldName)).append("() {\n");
        builder.append("        return delegate().get" + uppercase(fieldName) + "(ordinal);\n");
        builder.append("    }\n\n");

        builder.append("    public Double ").append(getterPrefix).append("get").append(uppercase(fieldName)).append("Boxed() {\n");
        builder.append("        return delegate().get" + uppercase(fieldName) + "Boxed(ordinal);\n");
        builder.append("    }");

        return builder.toString();
    }

    private String generateLongFieldAccessor(int fieldNum) {
        StringBuilder builder = new StringBuilder();

        String fieldName = substituteInvalidChars(schema.getFieldName(fieldNum));

        builder.append("    public long ").append(getterPrefix).append("get").append(uppercase(fieldName)).append("() {\n");
        builder.append("        return delegate().get" + uppercase(fieldName) + "(ordinal);\n");
        builder.append("    }\n\n");

        builder.append("    public Long ").append(getterPrefix).append("get").append(uppercase(fieldName)).append("Boxed() {\n");
        builder.append("        return delegate().get" + uppercase(fieldName) + "Boxed(ordinal);\n");
        builder.append("    }");

        return builder.toString();
    }

    private String generateIntFieldAccessor(int fieldNum) {
        StringBuilder builder = new StringBuilder();

        String fieldName = substituteInvalidChars(schema.getFieldName(fieldNum));

        builder.append("    public int ").append(getterPrefix).append("get").append(uppercase(fieldName)).append("() {\n");
        builder.append("        return delegate().get" + uppercase(fieldName) + "(ordinal);\n");
        builder.append("    }\n\n");

        builder.append("    public Integer ").append(getterPrefix).append("get").append(uppercase(fieldName)).append("Boxed() {\n");
        builder.append("        return delegate().get" + uppercase(fieldName) + "Boxed(ordinal);\n");
        builder.append("    }");

        return builder.toString();
    }

    private String generateBooleanFieldAccessor(int fieldNum) {
        StringBuilder builder = new StringBuilder();

        String fieldName = substituteInvalidChars(schema.getFieldName(fieldNum));

        builder.append("    public boolean ").append(getterPrefix).append("get").append(uppercase(fieldName)).append("() {\n");
        builder.append("        return delegate().get" + uppercase(fieldName) + "(ordinal);\n");
        builder.append("    }\n\n");

        builder.append("    public Boolean ").append(getterPrefix).append("get").append(uppercase(fieldName)).append("Boxed() {\n");
        builder.append("        return delegate().get" + uppercase(fieldName) + "Boxed(ordinal);\n");
        builder.append("    }");

        return builder.toString();
    }

    private void appendAPIAccessor(StringBuilder classBuilder) {
        classBuilder.append("    public " + apiClassname + " api() {\n");
        classBuilder.append("        return typeApi().getAPI();\n");
        classBuilder.append("    }\n\n");
    }

    private void appendTypeAPIAccessor(StringBuilder classBuilder) {
        String typeAPIClassname = typeAPIClassname(schema.getName());
        classBuilder.append("    public " + typeAPIClassname + " typeApi() {\n");
        classBuilder.append("        return delegate().getTypeAPI();\n");
        classBuilder.append("    }\n\n");
    }

    private void appendDelegateAccessor(StringBuilder classBuilder) {
        classBuilder.append("    protected ").append(delegateInterfaceName(schema.getName())).append(" delegate() {\n");
        classBuilder.append("        return (").append(delegateInterfaceName(schema.getName())).append(")delegate;\n");
        classBuilder.append("    }\n\n");
    }

}
