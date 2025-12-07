/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH
 * under one or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information regarding copyright
 * ownership. Camunda licenses this file to you under the Apache License,
 * Version 2.0; you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.operaton.bpm.model.dmn;

import java.io.*;
import java.util.ServiceLoader;

import org.operaton.bpm.model.dmn.impl.DmnParser;
import org.operaton.bpm.model.dmn.impl.instance.*;
import org.operaton.bpm.model.xml.Model;
import org.operaton.bpm.model.xml.ModelBuilder;
import org.operaton.bpm.model.xml.ModelException;
import org.operaton.bpm.model.xml.ModelParseException;
import org.operaton.bpm.model.xml.ModelValidationException;
import org.operaton.bpm.model.xml.impl.instance.ModelElementInstanceImpl;
import org.operaton.bpm.model.xml.impl.util.IoUtil;

import static org.operaton.bpm.model.dmn.impl.DmnModelConstants.*;

/**
 * <p>Provides access to the operaton DMN model api.</p>
 */
public class Dmn {

  /** The singleton instance of {@link Dmn} created by the first {@link DmnFactory} found via the {@link ServiceLoader}.
   * <p>To customize the behavior of Dmn, add the fully qualified class name of the custom implementation to the file:
   * {@code META-INF/services/org.operaton.bpm.model.dmn.DmnFactory}</p>. */
  public static final Dmn INSTANCE;

  /** {@link DmnParser} created by the first {@link DmnParserFactory} found via the {@link ServiceLoader}.
   * <p>To customize the behavior, provide a custom {@link DmnParserFactory} implementation and declare it in:
   * {@code META-INF/services/org.operaton.bpm.model.dmn.DmnParserFactory}.</p>
   */
  private static final DmnParser DMN_PARSER;

  static {
    DmnFactory dmnFactory = ServiceLoader.load(DmnFactory.class).findFirst().orElse(
      ServiceLoader.load(DmnFactory.class, Dmn.class.getClassLoader()).findFirst()
        .orElseThrow(() -> new IllegalStateException("No DmnFactory found"))
    );
    DmnParserFactory dmnParserFactory = ServiceLoader.load(DmnParserFactory.class).findFirst().orElse(
      ServiceLoader.load(DmnParserFactory.class, Dmn.class.getClassLoader()).findFirst()
        .orElseThrow(() -> new IllegalStateException("No DmnParserFactory found"))
    );

    INSTANCE = dmnFactory.newInstance();
    DMN_PARSER = dmnParserFactory.newInstance();
  }

  private final ModelBuilder dmnModelBuilder;

  /** The {@link Model}
   */
  private Model dmnModel;

  /**
   * Allows reading a {@link DmnModelInstance} from a File.
   *
   * @param file the {@link File} to read the {@link DmnModelInstance} from
   * @return the model read
   * @throws DmnModelException if the model cannot be read
   */
  public static DmnModelInstance readModelFromFile(File file) {
    return INSTANCE.doReadModelFromFile(file);
  }

  /**
   * Allows reading a {@link DmnModelInstance} from an {@link InputStream}
   *
   * @param stream the {@link InputStream} to read the {@link DmnModelInstance} from
   * @return the model read
   * @throws ModelParseException if the model cannot be read
   */
  public static DmnModelInstance readModelFromStream(InputStream stream) {
    return INSTANCE.doReadModelFromInputStream(stream);
  }

  /**
   * Allows writing a {@link DmnModelInstance} to a File. It will be
   * validated before writing.
   *
   * @param file the {@link File} to write the {@link DmnModelInstance} to
   * @param modelInstance the {@link DmnModelInstance} to write
   * @throws DmnModelException if the model cannot be written
   * @throws ModelValidationException if the model is not valid
   */
  public static void writeModelToFile(File file, DmnModelInstance modelInstance) {
    INSTANCE.doWriteModelToFile(file, modelInstance);
  }

  /**
   * Allows writing a {@link DmnModelInstance} to an {@link OutputStream}. It will be
   * validated before writing.
   *
   * @param stream the {@link OutputStream} to write the {@link DmnModelInstance} to
   * @param modelInstance the {@link DmnModelInstance} to write
   * @throws ModelException if the model cannot be written
   * @throws ModelValidationException if the model is not valid
   */
  public static void writeModelToStream(OutputStream stream, DmnModelInstance modelInstance) {
    INSTANCE.doWriteModelToOutputStream(stream, modelInstance);
  }

  /**
   * Allows the conversion of a {@link DmnModelInstance} to an {@link String}. It will
   * be validated before conversion.
   *
   * @param modelInstance  the model instance to convert
   * @return the XML string representation of the model instance
   */
  public static String convertToString(DmnModelInstance modelInstance) {
    return INSTANCE.doConvertToString(modelInstance);
  }

  /**
   * Validate model DOM document
   *
   * @param modelInstance the {@link DmnModelInstance} to validate
   * @throws ModelValidationException if the model is not valid
   */
  public static void validateModel(DmnModelInstance modelInstance) {
    INSTANCE.doValidateModel(modelInstance);
  }

  /**
   * Allows creating an new, empty {@link DmnModelInstance}.
   *
   * @return the empty model.
   */
  public static DmnModelInstance createEmptyModel() {
    return INSTANCE.doCreateEmptyModel();
  }

  /**
   * Register known types of the Dmn model
   */
  public Dmn() {
    dmnModelBuilder = ModelBuilder.createInstance("DMN Model")
            .alternativeNamespace(CAMUNDA_NS, OPERATON_NS)
            .alternativeNamespace(DMN15_NS, DMN13_NS)
            .alternativeNamespace(DMN14_NS, DMN13_NS)
            .alternativeNamespace(DMN13_ALTERNATIVE_NS, DMN13_NS)
            .alternativeNamespace(DMN12_NS, DMN13_NS)
            .alternativeNamespace(DMN11_NS, DMN13_NS)
            .alternativeNamespace(DMN11_ALTERNATIVE_NS, DMN13_NS);
    doRegisterTypes(dmnModelBuilder);
    dmnModel = dmnModelBuilder.build();
  }

  protected DmnModelInstance doReadModelFromFile(File file) {
    DmnModelInstance result = null;
    try (InputStream is = new FileInputStream(file)) {
      result = doReadModelFromInputStream(is);

    } catch (FileNotFoundException e) {
      throw new DmnModelException("Cannot read model from file " + file + ": file does not exist.");
    } catch (IOException e) {
      throw new DmnModelException("Cannot read model from file " + file, e);
    }
    return result;
  }

  protected DmnModelInstance doReadModelFromInputStream(InputStream is) {
    return DMN_PARSER.parseModelFromStream(is);
  }

  protected void doWriteModelToFile(File file, DmnModelInstance modelInstance) {
    try (OutputStream os = new FileOutputStream(file)) {
      doWriteModelToOutputStream(os, modelInstance);
    } catch (FileNotFoundException e) {
      throw new DmnModelException("Cannot write model to file " + file + ": file does not exist.");
    } catch (IOException e) {
      throw new DmnModelException("Cannot write model to file " + file, e);
    }
  }

  protected void doWriteModelToOutputStream(OutputStream os, DmnModelInstance modelInstance) {
    // validate DOM document
    doValidateModel(modelInstance);
    // write XML
    IoUtil.writeDocumentToOutputStream(modelInstance.getDocument(), os);
  }

  protected String doConvertToString(DmnModelInstance modelInstance) {
    // validate DOM document
    doValidateModel(modelInstance);
    // convert to XML string
    return IoUtil.convertXmlDocumentToString(modelInstance.getDocument());
  }

  protected void doValidateModel(DmnModelInstance modelInstance) {
    DMN_PARSER.validateModel(modelInstance.getDocument());
  }

  protected DmnModelInstance doCreateEmptyModel() {
    return DMN_PARSER.getEmptyModel();
  }

  protected void doRegisterTypes(ModelBuilder modelBuilder) {

    AllowedAnswersImpl.registerType(modelBuilder);
    AllowedValuesImpl.registerType(modelBuilder);
    ArtifactImpl.registerType(modelBuilder);
    AssociationImpl.registerType(modelBuilder);
    AuthorityRequirementImpl.registerType(modelBuilder);
    BindingImpl.registerType(modelBuilder);
    BusinessContextElementImpl.registerType(modelBuilder);
    BusinessKnowledgeModelImpl.registerType(modelBuilder);
    ColumnImpl.registerType(modelBuilder);
    ContextEntryImpl.registerType(modelBuilder);
    ContextImpl.registerType(modelBuilder);
    DecisionImpl.registerType(modelBuilder);
    DecisionMadeReferenceImpl.registerType(modelBuilder);
    DecisionMakerReferenceImpl.registerType(modelBuilder);
    DecisionOwnedReferenceImpl.registerType(modelBuilder);
    DecisionOwnerReferenceImpl.registerType(modelBuilder);
    DecisionRuleImpl.registerType(modelBuilder);
    DecisionServiceImpl.registerType(modelBuilder);
    DecisionTableImpl.registerType(modelBuilder);
    DefaultOutputEntryImpl.registerType(modelBuilder);
    DefinitionsImpl.registerType(modelBuilder);
    DescriptionImpl.registerType(modelBuilder);
    DmnElementImpl.registerType(modelBuilder);
    DmnElementReferenceImpl.registerType(modelBuilder);
    DrgElementImpl.registerType(modelBuilder);
    DrgElementReferenceImpl.registerType(modelBuilder);
    ElementCollectionImpl.registerType(modelBuilder);
    EncapsulatedDecisionReferenceImpl.registerType(modelBuilder);
    EncapsulatedLogicImpl.registerType(modelBuilder);
    ExpressionImpl.registerType(modelBuilder);
    ExtensionElementsImpl.registerType(modelBuilder);
    FormalParameterImpl.registerType(modelBuilder);
    FunctionDefinitionImpl.registerType(modelBuilder);
    ImpactedPerformanceIndicatorReferenceImpl.registerType(modelBuilder);
    ImpactingDecisionReferenceImpl.registerType(modelBuilder);
    ImportImpl.registerType(modelBuilder);
    ImportedElementImpl.registerType(modelBuilder);
    ImportedValuesImpl.registerType(modelBuilder);
    InformationItemImpl.registerType(modelBuilder);
    InformationRequirementImpl.registerType(modelBuilder);
    InputImpl.registerType(modelBuilder);
    InputClauseImpl.registerType(modelBuilder);
    InputDataImpl.registerType(modelBuilder);
    InputDataReferenceImpl.registerType(modelBuilder);
    InputDecisionReferenceImpl.registerType(modelBuilder);
    InputEntryImpl.registerType(modelBuilder);
    InputExpressionImpl.registerType(modelBuilder);
    InputValuesImpl.registerType(modelBuilder);
    InvocationImpl.registerType(modelBuilder);
    ItemComponentImpl.registerType(modelBuilder);
    ItemDefinitionImpl.registerType(modelBuilder);
    ItemDefinitionReferenceImpl.registerType(modelBuilder);
    KnowledgeRequirementImpl.registerType(modelBuilder);
    KnowledgeSourceImpl.registerType(modelBuilder);
    ListImpl.registerType(modelBuilder);
    LiteralExpressionImpl.registerType(modelBuilder);
    ModelElementInstanceImpl.registerType(modelBuilder);
    NamedElementImpl.registerType(modelBuilder);
    OrganizationUnitImpl.registerType(modelBuilder);
    OutputImpl.registerType(modelBuilder);
    OutputClauseImpl.registerType(modelBuilder);
    OutputDecisionReferenceImpl.registerType(modelBuilder);
    OutputEntryImpl.registerType(modelBuilder);
    OutputValuesImpl.registerType(modelBuilder);
    OwnerReferenceImpl.registerType(modelBuilder);
    ParameterImpl.registerType(modelBuilder);
    PerformanceIndicatorImpl.registerType(modelBuilder);
    QuestionImpl.registerType(modelBuilder);
    RelationImpl.registerType(modelBuilder);
    RequiredAuthorityReferenceImpl.registerType(modelBuilder);
    RequiredDecisionReferenceImpl.registerType(modelBuilder);
    RequiredInputReferenceImpl.registerType(modelBuilder);
    RequiredKnowledgeReferenceImpl.registerType(modelBuilder);
    RowImpl.registerType(modelBuilder);
    RuleImpl.registerType(modelBuilder);
    SourceRefImpl.registerType(modelBuilder);
    SupportedObjectiveReferenceImpl.registerType(modelBuilder);
    TargetRefImpl.registerType(modelBuilder);
    TextImpl.registerType(modelBuilder);
    TextAnnotationImpl.registerType(modelBuilder);
    TypeImpl.registerType(modelBuilder);
    TypeRefImpl.registerType(modelBuilder);
    UnaryTestsImpl.registerType(modelBuilder);
    UsingProcessReferenceImpl.registerType(modelBuilder);
    UsingTaskReferenceImpl.registerType(modelBuilder);
    VariableImpl.registerType(modelBuilder);

    /** operaton extensions */
  }

  /**
   * @return the {@link Model} instance to use
   */
  public Model getDmnModel() {
    return dmnModel;
  }

  public ModelBuilder getDmnModelBuilder() {
    return dmnModelBuilder;
  }

  /**
   * @param dmnModel the dmnModel to set
   */
  public void setDmnModel(Model dmnModel) {
    this.dmnModel = dmnModel;
  }

}
