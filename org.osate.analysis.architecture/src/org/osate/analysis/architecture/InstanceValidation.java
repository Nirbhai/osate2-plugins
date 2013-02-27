package org.osate.analysis.architecture;

import org.osate.aadl2.ComponentCategory;
import org.osate.aadl2.ComponentClassifier;
import org.osate.aadl2.Element;
import org.osate.aadl2.instance.ComponentInstance;
import org.osate.aadl2.instance.SystemInstance;
import org.osate.aadl2.modelsupport.errorreporting.AnalysisErrorReporterManager;
import org.osate.aadl2.modelsupport.modeltraversal.ForAllElement;
import org.osate.xtext.aadl2.properties.util.GetProperties;

public class InstanceValidation {
	private final AnalysisErrorReporterManager errManager;
	private boolean isOK = true;

	public InstanceValidation(final AnalysisErrorReporterManager errManager) {
		this.errManager = errManager;
	}
	
	public boolean checkReferenceProcessor(SystemInstance root){
		isOK = true;
		ForAllElement mal = new ForAllElement() {
			@Override
			protected void process(Element obj) {
				if (obj instanceof ComponentInstance){
					ComponentInstance ci = (ComponentInstance) obj;
					double et = GetProperties.getSpecifiedComputeExecutionTimeinSec(ci);
					if (et != 0){
						ComponentClassifier refproc = GetProperties.getReferenceProcessor(ci);
						if (refproc == null){
							isOK = false;
							errManager.error(ci, "Thread instance "+ci.getComponentInstancePath()+" has execution time, but no Reference_Processor. Please this property.");
						}
					}
				}
			}
		};
		mal.processPreOrderComponentInstance(root, ComponentCategory.THREAD);
		return isOK;
	}
	

}
