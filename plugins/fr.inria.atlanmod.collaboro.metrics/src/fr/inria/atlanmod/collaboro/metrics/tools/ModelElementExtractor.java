package fr.inria.atlanmod.collaboro.metrics.tools;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.emf.common.util.TreeIterator;
import org.eclipse.emf.ecore.EAttribute;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.EReference;

import fr.inria.atlanmod.collaboro.metrics.symbol.AttributeConcept;
import fr.inria.atlanmod.collaboro.metrics.symbol.AttributeSymbol;
import fr.inria.atlanmod.collaboro.metrics.symbol.ClassConcept;
import fr.inria.atlanmod.collaboro.metrics.symbol.ClassSymbol;
import fr.inria.atlanmod.collaboro.metrics.symbol.Concept;
import fr.inria.atlanmod.collaboro.metrics.symbol.ReferenceConcept;
import fr.inria.atlanmod.collaboro.metrics.symbol.ReferenceSymbol;
import fr.inria.atlanmod.collaboro.metrics.symbol.Symbol;
import fr.inria.atlanmod.collaboro.notation.AttributeValue;
import fr.inria.atlanmod.collaboro.notation.Composite;
import fr.inria.atlanmod.collaboro.notation.Definition;
import fr.inria.atlanmod.collaboro.notation.NotationElement;
import fr.inria.atlanmod.collaboro.notation.ReferenceValue;
import fr.inria.atlanmod.collaboro.notation.SyntaxOf;

public class ModelElementExtractor {
	
	private List<EObject> abstractSyntaxElements;
	private List<NotationElement> concreteSyntaxElements;
	private List<Symbol> concreteSymbols;
	
	private List<ClassConcept> classConcepts;
	private List<AttributeConcept> attributeConcepts;
	private List<ReferenceConcept> referenceConcepts;
	
	public ModelElementExtractor() {
		this.classConcepts = new ArrayList<ClassConcept>();
		this.attributeConcepts = new ArrayList<AttributeConcept>();
		this.referenceConcepts = new ArrayList<ReferenceConcept>();
	}
	
	public List<Concept> discoverAbstractConcepts(EPackage abstractSyntaxModel) {
		List<Concept> abstractConcepts = new ArrayList<Concept>();
		discoverAbstractClasses(abstractSyntaxModel);
		discoverAbstractAttribute(abstractSyntaxModel);
		discoverAbstractReference(abstractSyntaxModel);
		
		abstractConcepts.addAll(classConcepts);
		abstractConcepts.addAll(attributeConcepts);
		abstractConcepts.addAll(referenceConcepts);
		
		return abstractConcepts;
		
	}
	
	private void discoverAbstractClasses(EPackage abstractSyntaxModel) {
		List<EObject> abstractSyntaxContents = abstractSyntaxModel.eContents();
		for(EObject abstractSyntaxElement : abstractSyntaxContents) {
			if(abstractSyntaxElement instanceof EClass) {
				EClass eClassElement = (EClass) abstractSyntaxElement;
				ClassConcept classConcept = new ClassConcept(eClassElement.getName(), eClassElement.getName(), eClassElement);
				classConcepts.add(classConcept);
			}
		}
		
		// extracting heritage information
		resolveClassHeritage();
	}
	
	private void resolveClassHeritage() {
		for(ClassConcept classConcept : classConcepts) {
			EClass eClass = (EClass) classConcept.getAbstractModelElement();
			List<EClass> eClassSuperTypes = eClass.getESuperTypes();
			for(EClass eClassSuperType : eClassSuperTypes) {
				ClassConcept classConceptSuperType = getClassConceptById(eClassSuperType.getName());
				if(classConceptSuperType != null) {
					classConceptSuperType.addSubType(classConcept);
					classConcept.addSuperType(classConceptSuperType);
				}
			}
		}
	}
	
	private void discoverAbstractAttribute(EPackage abstractSyntaxModel) {
		for(ClassConcept classConcept : classConcepts) {
			EClass eClass = (EClass) classConcept.getAbstractModelElement();
			List<EAttribute> eClassAttributes = eClass.getEAllAttributes();
			for(EAttribute eClassAttribute : eClassAttributes) {
				String attributeName = eClass.getName() + "." + eClassAttribute.getName();
				AttributeConcept attributeConcept = new AttributeConcept(eClassAttribute.getName(), attributeName, eClassAttribute);
				attributeConcept.setClassConcept(classConcept);

				EClass eContainingClass = eClassAttribute.getEContainingClass();
				if(!eContainingClass.equals(eClass)) {
					ClassConcept containingClassConcept = getClassConceptById(eContainingClass.getName());
					if(containingClassConcept != null) {
						attributeConcept.setContainingSuperClass(containingClassConcept);
					}
				}
				this.attributeConcepts.add(attributeConcept);
				classConcept.addAttribute(attributeConcept);
			}
		}
		
		// extracting heritage information
		for(AttributeConcept attributeConcept : attributeConcepts) {
			ClassConcept attributeSuperClass = attributeConcept.getContainingSuperClass();
			if(attributeSuperClass == null) {
				// not heritated attribute
				resolveAttributeHeritage(attributeConcept);
			}
		}
	}
	
	private void resolveAttributeHeritage(AttributeConcept attributeConcept) {
		EObject modelAttribute = attributeConcept.getAbstractModelElement();
		ClassConcept containingClass = attributeConcept.getClassConcept();
		List<ClassConcept> containingClassSubClasses = containingClass.getSubTypes();
		for(ClassConcept subClassConcept : containingClassSubClasses) {
			AttributeConcept subAttributeConcept = getAttributeConceptByEObjectFromClassConcept(subClassConcept, modelAttribute);
			if(subAttributeConcept != null) {
				attributeConcept.addSubAttribute(subAttributeConcept);
				subAttributeConcept.addSuperAttribute(attributeConcept);
				resolveAttributeHeritage(subAttributeConcept);
			}
		}
		
	}
	
	private AttributeConcept getAttributeConceptByEObjectFromClassConcept(ClassConcept classConcept, EObject modelObject) {
		List<AttributeConcept> classAttributes = classConcept.getAttributes();
		for(AttributeConcept classAttribute : classAttributes) {
			if(classAttribute.getAbstractModelElement().equals(modelObject)) {
				return classAttribute;
			}
		}
		return null;
	}
	

	
	private void discoverAbstractReference(EPackage abstractSyntaxModel) {
		for(ClassConcept classConcept : classConcepts) {
			EClass eClass = (EClass) classConcept.getAbstractModelElement();
			List<EReference> eClassReferences = eClass.getEAllReferences();
			for(EReference eClassReference : eClassReferences) {
				String referenceName = eClass.getName() + "." + eClassReference.getName();
				ReferenceConcept referenceConcept = new ReferenceConcept(eClassReference.getName(), referenceName, eClassReference);
				
				referenceConcept.setContainingClass(classConcept);
				//From
				EClass eClassReferenceFrom = eClassReference.getEContainingClass();
				ClassConcept referenceClassConceptFrom = getClassConceptById(eClassReferenceFrom.getName());
				referenceConcept.setSuperClassConceptFrom(referenceClassConceptFrom);
				//To
				EClass eClassReferenceTo = eClassReference.getEReferenceType();
				ClassConcept referenceClassConceptTo = getClassConceptById(eClassReferenceTo.getName());
				referenceConcept.setClassConceptTo(referenceClassConceptTo);
				
				referenceConcept.setSuperClassConceptTo(referenceClassConceptTo);
				
				referenceConcepts.add(referenceConcept);
				classConcept.addReference(referenceConcept);
			}
		}
		
		//Opposite Discovery
		List<ReferenceConcept> oppositeReferenceConcepts = resolveReferenceConceptOpposite();
		referenceConcepts.addAll(oppositeReferenceConcepts);
		
		//extracting heritage information
		resolveReferenceHeritage();
		
		
	}
	
	private List<ReferenceConcept> resolveReferenceConceptOpposite() {
		List<ReferenceConcept> oppositeReferenceConcepts = new ArrayList<ReferenceConcept>();
		for(ReferenceConcept referenceConcept : referenceConcepts) {
			EReference eReference = (EReference) referenceConcept.getAbstractModelElement();
			EReference eOppositeReference = eReference.getEOpposite();
			if(eOppositeReference != null) {
				//To
				ClassConcept referenceContainingClass = referenceConcept.getContainingClass();
				EClass eOppositeReferenceToEClass = eOppositeReference.getEReferenceType();
				ClassConcept oppositeReferenceToClassConcept = getClassConceptById(eOppositeReferenceToEClass.getName());
				//From
				EClass eOppositeReferenceContainingClass = eOppositeReference.getEContainingClass();
				ClassConcept oppositeReferenceContainingClass = getClassConceptById(eOppositeReferenceContainingClass.getName());
				
				ReferenceConcept oppositeReferenceConcept = getExistingReferenceConcept(oppositeReferenceContainingClass,referenceContainingClass,eOppositeReference);
				
				if(oppositeReferenceConcept == null) {
					String oppositeReferenceName = eOppositeReferenceContainingClass.getName() + "." + eOppositeReference.getName();
					oppositeReferenceConcept = new ReferenceConcept(eOppositeReference.getName(), oppositeReferenceName, eOppositeReference);
					oppositeReferenceConcept.setContainingClass(oppositeReferenceContainingClass);
					oppositeReferenceConcept.setSuperClassConceptFrom(oppositeReferenceContainingClass);
					oppositeReferenceConcept.setSuperClassConceptTo(oppositeReferenceToClassConcept);
					oppositeReferenceConcept.setClassConceptTo(referenceContainingClass);
					oppositeReferenceConcept.setReferenceOpposite(referenceConcept);
					referenceConcept.setReferenceOpposite(oppositeReferenceConcept);
					oppositeReferenceConcepts.add(oppositeReferenceConcept);
					oppositeReferenceContainingClass.addReference(oppositeReferenceConcept);
				} else {
					referenceConcept.setReferenceOpposite(oppositeReferenceConcept);
				}
			}
		}
		return oppositeReferenceConcepts;
	}
	
	private void resolveReferenceHeritage() {
		for(ReferenceConcept referenceConcept : referenceConcepts) {
			ClassConcept referenceContainingClass = referenceConcept.getContainingClass();
			ClassConcept referenceSuperContainingClass = referenceConcept.getSuperClassConceptFrom();
			ClassConcept referenceToClass = referenceConcept.getClassConceptTo();
			ClassConcept referenceSuperToClass = referenceConcept.getSuperClassConceptTo();
			if(referenceSuperContainingClass.equals(referenceContainingClass)) {
				if(referenceSuperToClass.equals(referenceToClass)) {
					// not heritated Reference
					resolveRH(referenceConcept);
					resolveRH2(referenceConcept);
				}	
			}
		}
	}

	
	private void resolveRH(ReferenceConcept referenceConcept) {
		EReference modelReference = (EReference) referenceConcept.getAbstractModelElement();
		ClassConcept containingClass = referenceConcept.getContainingClass();
		ClassConcept referenceToSuperClass = referenceConcept.getSuperClassConceptTo();
		List<ClassConcept> containingClassSubClasses = containingClass.getSubTypes();
		for(ClassConcept subClassConcept : containingClassSubClasses) {
			ReferenceConcept subReferenceConcept = getExistingReferenceConcept(subClassConcept,referenceToSuperClass,modelReference);
			if(subReferenceConcept != null) {
				referenceConcept.addSubReference(subReferenceConcept);
				subReferenceConcept.addSuperReference(referenceConcept);
				resolveRH(subReferenceConcept);
			}
		}
	}
	
	private void resolveRH2(ReferenceConcept referenceConcept) {
		EReference modelReference = (EReference) referenceConcept.getAbstractModelElement();
		ClassConcept referenceFromClass = referenceConcept.getSuperClassConceptFrom();
		ClassConcept referenceToClass = referenceConcept.getClassConceptTo();
		List<ClassConcept> referenceToClassSubClasses = referenceToClass.getSubTypes();
		for(ClassConcept referenceToClassSubClass : referenceToClassSubClasses) {
			ReferenceConcept subReferenceConcept = getExistingReferenceConcept(referenceFromClass,referenceToClassSubClass,modelReference);
			if(subReferenceConcept != null) {
				subReferenceConcept.addSuperReference(referenceConcept);
				referenceConcept.addSubReference(subReferenceConcept);
				resolveRH2(subReferenceConcept);
			}
		}
	}
	
	private ReferenceConcept getExistingReferenceConcept(ClassConcept classConceptFrom,ClassConcept classConceptTo, EReference eReference) {
		List<ReferenceConcept> classConceptReferences = classConceptFrom.getReferences();
		for(ReferenceConcept classConceptReference : classConceptReferences) {
			ClassConcept classConceptReferenceTo = classConceptReference.getClassConceptTo();
			if(classConceptReferenceTo.equals(classConceptTo)) {
				EReference classConceptReferenceEReference = (EReference) classConceptReference.getAbstractModelElement();
				if(classConceptReferenceEReference.equals(eReference)) {
					return classConceptReference;
				}
			}
		}
		return null;
	}
	
	private ClassConcept getClassConceptById(String id) {
		for(ClassConcept classConcept : classConcepts) {
			if(classConcept.getAbstractModelId().equals(id)) {
				return classConcept;
			}
		}
		return null;
	}
	
	public void resolveHeritage(Map<String,EObject> abstractConcepts) {
		Set<String> abstractConceptsKeySet = abstractConcepts.keySet();
		for(String abstractConceptId : abstractConceptsKeySet) {
			EObject abstractConcept = abstractConcepts.get(abstractConceptId);
			
		}
		
		Collection<EObject> abstractSyntaxContents = abstractConcepts.values();
		for(EObject abstractSyntaxElement : abstractSyntaxContents) {
			if(abstractSyntaxElement instanceof EClass) {
				EClass eClass = (EClass) abstractSyntaxElement;
				List<EClass> eClassSuperClasses = eClass.getEAllSuperTypes();
				System.out.println("Class : " + eClass.getName());
				System.out.println(eClassSuperClasses);
			}
		}
	}
	
	
	public List<Symbol> discoverConcreteConcepts(Definition concreteSyntaxModel) {
		List<Symbol> concreteSymbols = new ArrayList<Symbol>();
		
		System.out.println("Discovery of Concrete Symbols : ");
		List<NotationElement> concreteSyntaxElements = concreteSyntaxModel.getElements();
		
		for(NotationElement concreteSyntaxElement : concreteSyntaxElements) {
			if(concreteSyntaxElement instanceof Composite) {
				Composite compositeElement = (Composite) concreteSyntaxElement;
				String compositeElementId = compositeElement.getId();
				boolean isClassComposite = false;
				boolean isAttributeComposite = false;
				boolean isReferenceComposite = false;
				// Check the type of the composite (Class, Attribute, Reference)
				String[] splitCompositeElementId = compositeElementId.split("\\.");
				if(splitCompositeElementId.length == 3) {
					// Reference symbol
					String referenceClassName = splitCompositeElementId[0];
					String referenceReferenceName = splitCompositeElementId[1];
					String referenceAttributeName = splitCompositeElementId[2];
					isReferenceComposite = true;
					/*Symbol referenceSymbol = new ReferenceSymbol(compositeElementId, referenceClassName, referenceReferenceName, referenceAttributeName, compositeElement);
					concreteSymbols.add(referenceSymbol);
					
					System.out.println("Found Reference : " + compositeElementId + " -> " + compositeElement);*/
				} else if (splitCompositeElementId.length == 2){
					// Attribute symbol
					String attributeClassName = splitCompositeElementId[0];
					String attributeAttributeName = splitCompositeElementId[1];
					isAttributeComposite = true;
					
					/*Symbol referenceSymbol = new AttributeSymbol(compositeElementId, attributeClassName, attributeAttributeName, compositeElement);
					concreteSymbols.add(referenceSymbol);
					System.out.println("Found Attribute : " + compositeElementId + " -> " + compositeElement);*/
				} else if (splitCompositeElementId.length == 1) {
					//Class symbol
					isClassComposite = true;
					String className = compositeElementId;
					
					Symbol classSymbol = new ClassSymbol(compositeElementId, compositeElementId, compositeElement);
					concreteSymbols.add(classSymbol);
					System.out.println("Found Class : " + compositeElementId + " -> " + compositeElement);
				}
				
				// Discovering component contents
				TreeIterator<EObject> compositeContents = compositeElement.eAllContents();
				while(compositeContents.hasNext()) {
					EObject compositeContent = compositeContents.next();
					if(compositeContent instanceof AttributeValue) {
						AttributeValue compositeAttributeValue = (AttributeValue) compositeContent;
						String attributeValueId = compositeAttributeValue.getId();
						EAttribute attributeValueEAttribute = compositeAttributeValue.getAttribute();
						String attributeValueClassName = "";
						String attributeValueAttributeName = "";
						if(attributeValueEAttribute != null) {
							//TODO Check 
							attributeValueClassName = attributeValueEAttribute.getEContainingClass().getName();
							attributeValueAttributeName = attributeValueEAttribute.getName();
						} else {
							String[] splitAttributeValueId = attributeValueId.split("\\.");
							attributeValueClassName = splitAttributeValueId[0];
							attributeValueAttributeName = splitAttributeValueId[1];
						}
						Symbol attributeSymbol = new AttributeSymbol(attributeValueId, attributeValueClassName, attributeValueAttributeName, compositeAttributeValue);
						concreteSymbols.add(attributeSymbol);
						System.out.println("Found Attribute : " + attributeValueId + " -> " + compositeAttributeValue);
						
						
					} else if (compositeContent instanceof ReferenceValue) {
						ReferenceValue compositeReferenceValue = (ReferenceValue) compositeContent;
						String referenceValueId = compositeReferenceValue.getId();
						EAttribute referenceValueEAttribute = compositeReferenceValue.getAttribute();
						EReference referenceValueEReference = compositeReferenceValue.getReference();
						String referenceValueClassName = "";
						String referenceValueReferenceName = "";
						String referenceValueAttributeName = "";
						if(referenceValueEReference != null) {
							referenceValueClassName = referenceValueEReference.getEContainingClass().getName();
							referenceValueReferenceName = referenceValueEReference.getName();
							if(referenceValueEAttribute != null) {
								referenceValueAttributeName = referenceValueEAttribute.getName();
							}
							
						} else {
							String[] splitReferenceValueId = referenceValueId.split("\\.");
							if(splitReferenceValueId.length == 3) {
								referenceValueClassName = splitReferenceValueId[0];
								referenceValueReferenceName = splitReferenceValueId[1];
								referenceValueAttributeName = splitReferenceValueId[2];
							}
						}
						
						Symbol referenceSymbol = new ReferenceSymbol(referenceValueId, referenceValueClassName, referenceValueReferenceName, referenceValueAttributeName, compositeReferenceValue);
						concreteSymbols.add(referenceSymbol);
						System.out.println("Found Reference : " + referenceValueId + " -> " + compositeReferenceValue);
						
						
					} else if (compositeContent instanceof SyntaxOf) {
						SyntaxOf compositeSyntaxOf = (SyntaxOf) compositeContent;
						String syntaxOfId = compositeSyntaxOf.getId();
						EReference syntaxOfEReference = compositeSyntaxOf.getReference();
						if(syntaxOfEReference != null) {
							
						} else {
							String[] splitSyntaxOfId = syntaxOfId.split("\\.");
							if(splitSyntaxOfId.length == 3) {
								//Reference syntax
								String syntaxOfClassName = splitSyntaxOfId[0];
								if(isClassComposite) {
									syntaxOfClassName = compositeElementId;
								}
								String syntaxOfReferenceName = splitSyntaxOfId[1];
								String syntaxOfAttributeName = splitSyntaxOfId[2];
								String syntaxOfName = syntaxOfClassName + "." + syntaxOfReferenceName + "." + syntaxOfAttributeName;
								Symbol referenceSymbol = new ReferenceSymbol(syntaxOfName, syntaxOfClassName, syntaxOfReferenceName, syntaxOfAttributeName, compositeSyntaxOf);
								concreteSymbols.add(referenceSymbol);
								System.out.println("Found Reference : " + syntaxOfName + " -> " + compositeSyntaxOf);
								
							} else if(splitSyntaxOfId.length == 2) {
								// Attribute syntax
								String syntaxOfClassName = splitSyntaxOfId[0];
								String syntaxOfAttributeName = splitSyntaxOfId[1];
								
								if(isClassComposite) {
									syntaxOfClassName = compositeElementId;
								}
								String syntaxOfName = syntaxOfClassName + "." + syntaxOfAttributeName;
								Symbol attributeSymbol = new AttributeSymbol(syntaxOfName, syntaxOfClassName, syntaxOfAttributeName, compositeSyntaxOf);
								concreteSymbols.add(attributeSymbol);
								System.out.println("Found Attribute : " + syntaxOfName + " -> " + compositeSyntaxOf);
								
							} else if(splitSyntaxOfId.length == 1) {
								// Class syntax
							}
						}
						
					}
				}
			}
		}
		
		return concreteSymbols;
	}
	
	private List<Symbol> primarySymbols;
	
	private void resolveComposite(Composite composite) {
		String compositeElementId = composite.getId();
		boolean isClassComposite = false;
		boolean isAttributeComposite = false;
		boolean isReferenceComposite = false;
		// Check the type of the composite (Class, Attribute, Reference)
		String[] splitCompositeElementId = compositeElementId.split("\\.");
		if(splitCompositeElementId.length == 3) {
			// Reference symbol
			String referenceClassName = splitCompositeElementId[0];
			String referenceReferenceName = splitCompositeElementId[1];
			String referenceAttributeName = splitCompositeElementId[2];
			isReferenceComposite = true;
			Symbol referenceSymbol = new ReferenceSymbol(compositeElementId, referenceClassName, referenceReferenceName, referenceAttributeName, composite);
			concreteSymbols.add(referenceSymbol);
			
			System.out.println("Found Reference : " + compositeElementId + " -> " + composite);
		} else if (splitCompositeElementId.length == 2){
			// Attribute symbol
			String attributeClassName = splitCompositeElementId[0];
			String attributeAttributeName = splitCompositeElementId[1];
			isAttributeComposite = true;
			
			Symbol referenceSymbol = new AttributeSymbol(compositeElementId, attributeClassName, attributeAttributeName, composite);
			concreteSymbols.add(referenceSymbol);
			System.out.println("Found Attribute : " + compositeElementId + " -> " + composite);
		} else if (splitCompositeElementId.length == 1) {
			//Class symbol
			isClassComposite = true;
			String className = compositeElementId;
			Symbol classSymbol = new ClassSymbol(compositeElementId, compositeElementId, composite);
			concreteSymbols.add(classSymbol);
			System.out.println("Found Class : " + compositeElementId + " -> " + composite);
		}
	}
	
	private boolean checkExistingAttributeConcept(String attributeName) {
		
		return false;
	}
	
	private void resolveAttribute() {
		
	}
	
	private void resolveReference() {
		
	}
	
	
	
	
	
	private void resolveSyntaxOf() {
		
	}
	
	public ModelMapping getModelMapping(EPackage abstractSyntaxModel, Definition concreteSyntaxModel) {
		List<Concept> abstractConcepts = discoverAbstractConcepts(abstractSyntaxModel);
		//Map<String,EObject> abstractConcepts = discoverAbstractConcepts(abstractSyntaxModel);
		//resolveHeritage(abstractConcepts);
		//System.out.println(abstractConcepts);
		this.concreteSymbols = discoverConcreteConcepts(concreteSyntaxModel);
		//System.out.println(concreteSymbols);
		ModelMapping modelMapping = new ModelMapping(abstractConcepts, concreteSymbols);
		//System.out.println(modelMapping);
		return modelMapping;
	}
	
	//TODO debug
	public void printResult() {
		for(ClassConcept classConcept : classConcepts) {
			System.out.println("Class concept : " + classConcept.getAbstractModelId() +  " , " + classConcept.getName());
			System.out.println("\tsubTypes : " + classConcept.getSubTypes());
			System.out.println("\tsuperTypes : " + classConcept.getSuperType());
			System.out.println("\tattributes : " + classConcept.getAttributes());
			System.out.println("\treferences : " + classConcept.getReferences());
		}
		for(AttributeConcept attribute : attributeConcepts) {
			System.out.println("Attribute Concept : " + attribute.getAbstractModelId() + " , " + attribute.getName());
			System.out.println("\tcontainingClass : " + attribute.getClassConcept());
			System.out.println("\tsuper containingClass : " + attribute.getContainingSuperClass());
			System.out.println("\tsubAttributes : " + attribute.getSubAttributes());
			System.out.println("\tsuperAttributes : " + attribute.getSuperAttributes());
		}
		for(ReferenceConcept reference : referenceConcepts) {
			System.out.println("Reference Concept : " + reference.getAbstractModelId() + " , " + reference.getName());
			System.out.println("\tcontaining class : " + reference.getContainingClass());
			System.out.println("\tSuper from class : " + reference.getSuperClassConceptFrom());
			System.out.println("\tSuper to class : " + reference.getSuperClassConceptTo());
			System.out.println("\tto class : " + reference.getClassConceptTo());
			if(reference.getReferenceOpposite() != null) {
				System.out.println("\tOpposite : " + reference.getReferenceOpposite() + "." + reference.getReferenceOpposite().getClassConceptTo());
			} else {
				System.out.println("\tOpposite : " + reference.getReferenceOpposite());
			}
			
			System.out.println("\tsubReferences : " + reference.getSubReferences());
			System.out.println("\tsuperReferences : " + reference.getSuperReferences());
		}
		
	}
	
}
