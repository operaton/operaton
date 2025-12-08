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
package org.operaton.bpm.model.cmmn;

import java.io.*;
import java.util.ServiceLoader;

import org.operaton.bpm.model.cmmn.impl.CmmnParser;
import org.operaton.bpm.model.cmmn.impl.instance.ApplicabilityRuleImpl;
import org.operaton.bpm.model.cmmn.impl.instance.ArtifactImpl;
import org.operaton.bpm.model.cmmn.impl.instance.AssociationImpl;
import org.operaton.bpm.model.cmmn.impl.instance.BindingRefinementExpressionImpl;
import org.operaton.bpm.model.cmmn.impl.instance.BodyImpl;
import org.operaton.bpm.model.cmmn.impl.instance.CaseFileImpl;
import org.operaton.bpm.model.cmmn.impl.instance.CaseFileItemDefinitionImpl;
import org.operaton.bpm.model.cmmn.impl.instance.CaseFileItemImpl;
import org.operaton.bpm.model.cmmn.impl.instance.CaseFileItemOnPartImpl;
import org.operaton.bpm.model.cmmn.impl.instance.CaseFileItemStartTriggerImpl;
import org.operaton.bpm.model.cmmn.impl.instance.CaseFileItemTransitionStandardEventImpl;
import org.operaton.bpm.model.cmmn.impl.instance.CaseFileModelImpl;
import org.operaton.bpm.model.cmmn.impl.instance.CaseImpl;
import org.operaton.bpm.model.cmmn.impl.instance.CaseParameterImpl;
import org.operaton.bpm.model.cmmn.impl.instance.CasePlanModel;
import org.operaton.bpm.model.cmmn.impl.instance.CaseRefExpressionImpl;
import org.operaton.bpm.model.cmmn.impl.instance.CaseRoleImpl;
import org.operaton.bpm.model.cmmn.impl.instance.CaseRolesImpl;
import org.operaton.bpm.model.cmmn.impl.instance.CaseTaskImpl;
import org.operaton.bpm.model.cmmn.impl.instance.ChildrenImpl;
import org.operaton.bpm.model.cmmn.impl.instance.CmmnElementImpl;
import org.operaton.bpm.model.cmmn.impl.instance.ConditionExpressionImpl;
import org.operaton.bpm.model.cmmn.impl.instance.CriterionImpl;
import org.operaton.bpm.model.cmmn.impl.instance.DecisionImpl;
import org.operaton.bpm.model.cmmn.impl.instance.DecisionParameterImpl;
import org.operaton.bpm.model.cmmn.impl.instance.DecisionRefExpressionImpl;
import org.operaton.bpm.model.cmmn.impl.instance.DecisionTaskImpl;
import org.operaton.bpm.model.cmmn.impl.instance.DefaultControlImpl;
import org.operaton.bpm.model.cmmn.impl.instance.DefinitionsImpl;
import org.operaton.bpm.model.cmmn.impl.instance.DiscretionaryItemImpl;
import org.operaton.bpm.model.cmmn.impl.instance.DocumentationImpl;
import org.operaton.bpm.model.cmmn.impl.instance.EntryCriterionImpl;
import org.operaton.bpm.model.cmmn.impl.instance.EventImpl;
import org.operaton.bpm.model.cmmn.impl.instance.EventListenerImpl;
import org.operaton.bpm.model.cmmn.impl.instance.ExitCriterionImpl;
import org.operaton.bpm.model.cmmn.impl.instance.ExpressionImpl;
import org.operaton.bpm.model.cmmn.impl.instance.ExtensionElementsImpl;
import org.operaton.bpm.model.cmmn.impl.instance.HumanTaskImpl;
import org.operaton.bpm.model.cmmn.impl.instance.IfPartImpl;
import org.operaton.bpm.model.cmmn.impl.instance.ImportImpl;
import org.operaton.bpm.model.cmmn.impl.instance.InputCaseParameterImpl;
import org.operaton.bpm.model.cmmn.impl.instance.InputDecisionParameterImpl;
import org.operaton.bpm.model.cmmn.impl.instance.InputProcessParameterImpl;
import org.operaton.bpm.model.cmmn.impl.instance.InputsCaseParameterImpl;
import org.operaton.bpm.model.cmmn.impl.instance.ItemControlImpl;
import org.operaton.bpm.model.cmmn.impl.instance.ManualActivationRuleImpl;
import org.operaton.bpm.model.cmmn.impl.instance.MilestoneImpl;
import org.operaton.bpm.model.cmmn.impl.instance.OnPartImpl;
import org.operaton.bpm.model.cmmn.impl.instance.OutputCaseParameterImpl;
import org.operaton.bpm.model.cmmn.impl.instance.OutputDecisionParameterImpl;
import org.operaton.bpm.model.cmmn.impl.instance.OutputProcessParameterImpl;
import org.operaton.bpm.model.cmmn.impl.instance.OutputsCaseParameterImpl;
import org.operaton.bpm.model.cmmn.impl.instance.ParameterImpl;
import org.operaton.bpm.model.cmmn.impl.instance.ParameterMappingImpl;
import org.operaton.bpm.model.cmmn.impl.instance.PlanFragmentImpl;
import org.operaton.bpm.model.cmmn.impl.instance.PlanItemControlImpl;
import org.operaton.bpm.model.cmmn.impl.instance.PlanItemDefinitionImpl;
import org.operaton.bpm.model.cmmn.impl.instance.PlanItemImpl;
import org.operaton.bpm.model.cmmn.impl.instance.PlanItemOnPartImpl;
import org.operaton.bpm.model.cmmn.impl.instance.PlanItemStartTriggerImpl;
import org.operaton.bpm.model.cmmn.impl.instance.PlanItemTransitionStandardEventImpl;
import org.operaton.bpm.model.cmmn.impl.instance.PlanningTableImpl;
import org.operaton.bpm.model.cmmn.impl.instance.ProcessImpl;
import org.operaton.bpm.model.cmmn.impl.instance.ProcessParameterImpl;
import org.operaton.bpm.model.cmmn.impl.instance.ProcessRefExpressionImpl;
import org.operaton.bpm.model.cmmn.impl.instance.ProcessTaskImpl;
import org.operaton.bpm.model.cmmn.impl.instance.PropertyImpl;
import org.operaton.bpm.model.cmmn.impl.instance.RelationshipImpl;
import org.operaton.bpm.model.cmmn.impl.instance.RepetitionRuleImpl;
import org.operaton.bpm.model.cmmn.impl.instance.RequiredRuleImpl;
import org.operaton.bpm.model.cmmn.impl.instance.RoleImpl;
import org.operaton.bpm.model.cmmn.impl.instance.SentryImpl;
import org.operaton.bpm.model.cmmn.impl.instance.SourceImpl;
import org.operaton.bpm.model.cmmn.impl.instance.StageImpl;
import org.operaton.bpm.model.cmmn.impl.instance.StartTriggerImpl;
import org.operaton.bpm.model.cmmn.impl.instance.TableItemImpl;
import org.operaton.bpm.model.cmmn.impl.instance.TargetImpl;
import org.operaton.bpm.model.cmmn.impl.instance.TaskImpl;
import org.operaton.bpm.model.cmmn.impl.instance.TextAnnotationImpl;
import org.operaton.bpm.model.cmmn.impl.instance.TextImpl;
import org.operaton.bpm.model.cmmn.impl.instance.TimerEventImpl;
import org.operaton.bpm.model.cmmn.impl.instance.TimerEventListenerImpl;
import org.operaton.bpm.model.cmmn.impl.instance.TimerExpressionImpl;
import org.operaton.bpm.model.cmmn.impl.instance.TransformationExpressionImpl;
import org.operaton.bpm.model.cmmn.impl.instance.UserEventImpl;
import org.operaton.bpm.model.cmmn.impl.instance.UserEventListenerImpl;
import org.operaton.bpm.model.cmmn.impl.instance.operaton.OperatonCaseExecutionListenerImpl;
import org.operaton.bpm.model.cmmn.impl.instance.operaton.OperatonExpressionImpl;
import org.operaton.bpm.model.cmmn.impl.instance.operaton.OperatonFieldImpl;
import org.operaton.bpm.model.cmmn.impl.instance.operaton.OperatonInImpl;
import org.operaton.bpm.model.cmmn.impl.instance.operaton.OperatonOutImpl;
import org.operaton.bpm.model.cmmn.impl.instance.operaton.OperatonScriptImpl;
import org.operaton.bpm.model.cmmn.impl.instance.operaton.OperatonStringImpl;
import org.operaton.bpm.model.cmmn.impl.instance.operaton.OperatonTaskListenerImpl;
import org.operaton.bpm.model.cmmn.impl.instance.operaton.OperatonVariableListenerImpl;
import org.operaton.bpm.model.cmmn.impl.instance.operaton.OperatonVariableOnPartImpl;
import org.operaton.bpm.model.cmmn.impl.instance.operaton.OperatonVariableTransitionEventImpl;
import org.operaton.bpm.model.xml.Model;
import org.operaton.bpm.model.xml.ModelBuilder;
import org.operaton.bpm.model.xml.ModelException;
import org.operaton.bpm.model.xml.ModelParseException;
import org.operaton.bpm.model.xml.ModelValidationException;
import org.operaton.bpm.model.xml.impl.instance.ModelElementInstanceImpl;
import org.operaton.bpm.model.xml.impl.util.IoUtil;
import org.operaton.commons.utils.ServiceLoaderUtil;

import static org.operaton.bpm.model.cmmn.impl.CmmnModelConstants.*;

/**
 * <p>Provides access to the operaton CMMN model api.</p>
 *
 * @author Roman Smirnov
 *
 */
public class Cmmn {

  /** The singleton instance of {@link Cmmn} created by the first {@link CmmnFactory} found via the {@link ServiceLoader}.
   * <p>To customize the behavior of Cmmn, add the fully qualified class name of the custom implementation to the file:
   * {@code META-INF/services/org.operaton.bpm.model.cmmn.CmmnFactory}</p>. */
  public static final Cmmn INSTANCE;

  /** {@link CmmnParser} created by the first {@link CmmnParserFactory} found via the {@link ServiceLoader}.
   * <p>To customize the behavior, provide a custom {@link CmmnParserFactory} implementation and declare it in:
   * {@code META-INF/services/org.operaton.bpm.model.cmmn.CmmnParserFactory}.</p>
   */
  private static final CmmnParser CMMN_PARSER;

  static {
    CmmnFactory cmmnFactory = ServiceLoaderUtil.loadSingleService(CmmnFactory.class);
    CmmnParserFactory cmmnParserFactory = ServiceLoaderUtil.loadSingleService(CmmnParserFactory.class);

    INSTANCE = cmmnFactory.newInstance();
    CMMN_PARSER = cmmnParserFactory.newInstance();
  }

  private final ModelBuilder cmmnModelBuilder;

  /** The {@link Model}
   */
  private Model cmmnModel;

  /**
   * Allows reading a {@link CmmnModelInstance} from a File.
   *
   * @param file the {@link File} to read the {@link CmmnModelInstance} from
   * @return the model read
   * @throws CmmnModelException if the model cannot be read
   */
  public static CmmnModelInstance readModelFromFile(File file) {
    return INSTANCE.doReadModelFromFile(file);
  }

  /**
   * Allows reading a {@link CmmnModelInstance} from an {@link InputStream}
   *
   * @param stream the {@link InputStream} to read the {@link CmmnModelInstance} from
   * @return the model read
   * @throws ModelParseException if the model cannot be read
   */
  public static CmmnModelInstance readModelFromStream(InputStream stream) {
    return INSTANCE.doReadModelFromInputStream(stream);
  }

  /**
   * Allows writing a {@link CmmnModelInstance} to a File. It will be
   * validated before writing.
   *
   * @param file the {@link File} to write the {@link CmmnModelInstance} to
   * @param modelInstance the {@link CmmnModelInstance} to write
   * @throws CmmnModelException if the model cannot be written
   * @throws ModelValidationException if the model is not valid
   */
  public static void writeModelToFile(File file, CmmnModelInstance modelInstance) {
    INSTANCE.doWriteModelToFile(file, modelInstance);
  }

  /**
   * Allows writing a {@link CmmnModelInstance} to an {@link OutputStream}. It will be
   * validated before writing.
   *
   * @param stream the {@link OutputStream} to write the {@link CmmnModelInstance} to
   * @param modelInstance the {@link CmmnModelInstance} to write
   * @throws ModelException if the model cannot be written
   * @throws ModelValidationException if the model is not valid
   */
  public static void writeModelToStream(OutputStream stream, CmmnModelInstance modelInstance) {
    INSTANCE.doWriteModelToOutputStream(stream, modelInstance);
  }

  /**
   * Allows the conversion of a {@link CmmnModelInstance} to an {@link String}. It will
   * be validated before conversion.
   *
   * @param modelInstance  the model instance to convert
   * @return the XML string representation of the model instance
   */
  public static String convertToString(CmmnModelInstance modelInstance) {
    return INSTANCE.doConvertToString(modelInstance);
  }

  /**
   * Validate model DOM document
   *
   * @param modelInstance the {@link CmmnModelInstance} to validate
   * @throws ModelValidationException if the model is not valid
   */
  public static void validateModel(CmmnModelInstance modelInstance) {
    INSTANCE.doValidateModel(modelInstance);
  }

  /**
   * Allows creating an new, empty {@link CmmnModelInstance}.
   *
   * @return the empty model.
   */
  public static CmmnModelInstance createEmptyModel() {
    return INSTANCE.doCreateEmptyModel();
  }

  /**
   * Register known types of the Cmmn model
   */
  public Cmmn() {
    cmmnModelBuilder = ModelBuilder.createInstance("CMMN Model")
            .alternativeNamespace(CAMUNDA_NS, OPERATON_NS)
            .alternativeNamespace(CMMN10_NS, CMMN11_NS);
    doRegisterTypes(cmmnModelBuilder);
    cmmnModel = cmmnModelBuilder.build();
  }

  protected CmmnModelInstance doReadModelFromFile(File file) {
    CmmnModelInstance result = null;
    try (InputStream is = new FileInputStream(file)) {
      result = doReadModelFromInputStream(is);
    } catch (FileNotFoundException e) {
      throw new CmmnModelException("Cannot read model from file " + file + ": file does not exist.");
    } catch (IOException e) {
      throw new CmmnModelException("Cannot read model from file " + file, e);
    }
    return result;
  }

  protected CmmnModelInstance doReadModelFromInputStream(InputStream is) {
    return CMMN_PARSER.parseModelFromStream(is);
  }

  protected void doWriteModelToFile(File file, CmmnModelInstance modelInstance) {
    try (OutputStream os = new FileOutputStream(file)) {
      doWriteModelToOutputStream(os, modelInstance);
    } catch (FileNotFoundException e) {
      throw new CmmnModelException("Cannot write model to file " + file + ": file does not exist.");
    } catch (IOException e) {
      throw new CmmnModelException("Cannot write model to file " + file, e);
    }
  }

  protected void doWriteModelToOutputStream(OutputStream os, CmmnModelInstance modelInstance) {
    // validate DOM document
    doValidateModel(modelInstance);
    // write XML
    IoUtil.writeDocumentToOutputStream(modelInstance.getDocument(), os);
  }

  protected String doConvertToString(CmmnModelInstance modelInstance) {
    // validate DOM document
    doValidateModel(modelInstance);
    // convert to XML string
    return IoUtil.convertXmlDocumentToString(modelInstance.getDocument());
  }

  protected void doValidateModel(CmmnModelInstance modelInstance) {
    CMMN_PARSER.validateModel(modelInstance.getDocument());
  }

  protected CmmnModelInstance doCreateEmptyModel() {
    return CMMN_PARSER.getEmptyModel();
  }

  protected void doRegisterTypes(ModelBuilder modelBuilder) {
    ArtifactImpl.registerType(modelBuilder);
    ApplicabilityRuleImpl.registerType(modelBuilder);
    AssociationImpl.registerType(modelBuilder);
    BindingRefinementExpressionImpl.registerType(modelBuilder);
    BodyImpl.registerType(modelBuilder);
    CaseFileImpl.registerType(modelBuilder);
    CaseFileItemDefinitionImpl.registerType(modelBuilder);
    CaseFileItemImpl.registerType(modelBuilder);
    CaseFileItemOnPartImpl.registerType(modelBuilder);
    CaseFileItemStartTriggerImpl.registerType(modelBuilder);
    CaseFileItemTransitionStandardEventImpl.registerType(modelBuilder);
    CaseFileModelImpl.registerType(modelBuilder);
    CaseImpl.registerType(modelBuilder);
    CaseParameterImpl.registerType(modelBuilder);
    CasePlanModel.registerType(modelBuilder);
    CaseRoleImpl.registerType(modelBuilder);
    CaseRolesImpl.registerType(modelBuilder);
    CaseRefExpressionImpl.registerType(modelBuilder);
    CaseTaskImpl.registerType(modelBuilder);
    ChildrenImpl.registerType(modelBuilder);
    CmmnElementImpl.registerType(modelBuilder);
    ConditionExpressionImpl.registerType(modelBuilder);
    CriterionImpl.registerType(modelBuilder);
    DecisionImpl.registerType(modelBuilder);
    DecisionParameterImpl.registerType(modelBuilder);
    DecisionRefExpressionImpl.registerType(modelBuilder);
    DecisionTaskImpl.registerType(modelBuilder);
    DefaultControlImpl.registerType(modelBuilder);
    DefinitionsImpl.registerType(modelBuilder);
    DiscretionaryItemImpl.registerType(modelBuilder);
    DocumentationImpl.registerType(modelBuilder);
    EntryCriterionImpl.registerType(modelBuilder);
    EventImpl.registerType(modelBuilder);
    EventListenerImpl.registerType(modelBuilder);
    ExitCriterionImpl.registerType(modelBuilder);
    ExpressionImpl.registerType(modelBuilder);
    ExtensionElementsImpl.registerType(modelBuilder);
    HumanTaskImpl.registerType(modelBuilder);
    IfPartImpl.registerType(modelBuilder);
    ImportImpl.registerType(modelBuilder);
    InputCaseParameterImpl.registerType(modelBuilder);
    InputProcessParameterImpl.registerType(modelBuilder);
    InputsCaseParameterImpl.registerType(modelBuilder);
    InputDecisionParameterImpl.registerType(modelBuilder);
    InputProcessParameterImpl.registerType(modelBuilder);
    ItemControlImpl.registerType(modelBuilder);
    ManualActivationRuleImpl.registerType(modelBuilder);
    MilestoneImpl.registerType(modelBuilder);
    ModelElementInstanceImpl.registerType(modelBuilder);
    OnPartImpl.registerType(modelBuilder);
    OutputCaseParameterImpl.registerType(modelBuilder);
    OutputProcessParameterImpl.registerType(modelBuilder);
    OutputsCaseParameterImpl.registerType(modelBuilder);
    OutputDecisionParameterImpl.registerType(modelBuilder);
    OutputProcessParameterImpl.registerType(modelBuilder);
    ParameterImpl.registerType(modelBuilder);
    ParameterMappingImpl.registerType(modelBuilder);
    PlanFragmentImpl.registerType(modelBuilder);
    PlanItemControlImpl.registerType(modelBuilder);
    PlanItemDefinitionImpl.registerType(modelBuilder);
    PlanItemImpl.registerType(modelBuilder);
    PlanItemOnPartImpl.registerType(modelBuilder);
    PlanItemStartTriggerImpl.registerType(modelBuilder);
    PlanItemTransitionStandardEventImpl.registerType(modelBuilder);
    PlanningTableImpl.registerType(modelBuilder);
    ProcessImpl.registerType(modelBuilder);
    ProcessParameterImpl.registerType(modelBuilder);
    ProcessRefExpressionImpl.registerType(modelBuilder);
    ProcessTaskImpl.registerType(modelBuilder);
    PropertyImpl.registerType(modelBuilder);
    RelationshipImpl.registerType(modelBuilder);
    RepetitionRuleImpl.registerType(modelBuilder);
    RequiredRuleImpl.registerType(modelBuilder);
    RoleImpl.registerType(modelBuilder);
    SentryImpl.registerType(modelBuilder);
    SourceImpl.registerType(modelBuilder);
    StageImpl.registerType(modelBuilder);
    StartTriggerImpl.registerType(modelBuilder);
    TableItemImpl.registerType(modelBuilder);
    TargetImpl.registerType(modelBuilder);
    TaskImpl.registerType(modelBuilder);
    TextAnnotationImpl.registerType(modelBuilder);
    TextImpl.registerType(modelBuilder);
    TimerEventImpl.registerType(modelBuilder);
    TimerEventListenerImpl.registerType(modelBuilder);
    TransformationExpressionImpl.registerType(modelBuilder);
    TimerExpressionImpl.registerType(modelBuilder);
    TransformationExpressionImpl.registerType(modelBuilder);
    UserEventImpl.registerType(modelBuilder);
    UserEventListenerImpl.registerType(modelBuilder);

    /** operaton extensions */
    OperatonCaseExecutionListenerImpl.registerType(modelBuilder);
    OperatonExpressionImpl.registerType(modelBuilder);
    OperatonFieldImpl.registerType(modelBuilder);
    OperatonInImpl.registerType(modelBuilder);
    OperatonOutImpl.registerType(modelBuilder);
    OperatonScriptImpl.registerType(modelBuilder);
    OperatonStringImpl.registerType(modelBuilder);
    OperatonTaskListenerImpl.registerType(modelBuilder);
    OperatonVariableListenerImpl.registerType(modelBuilder);
    OperatonVariableOnPartImpl.registerType(modelBuilder);
    OperatonVariableTransitionEventImpl.registerType(modelBuilder);
  }

  /**
   * @return the {@link Model} instance to use
   */
  public Model getCmmnModel() {
    return cmmnModel;
  }

  public ModelBuilder getCmmnModelBuilder() {
    return cmmnModelBuilder;
  }

  /**
   * @param cmmnModel the cmmnModel to set
   */
  public void setCmmnModel(Model cmmnModel) {
    this.cmmnModel = cmmnModel;
  }

}
