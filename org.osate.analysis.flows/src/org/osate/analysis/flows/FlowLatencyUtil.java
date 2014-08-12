package org.osate.analysis.flows;

import org.osate.aadl2.instance.ComponentInstance;
import org.osate.aadl2.instance.ConnectionInstance;
import org.osate.aadl2.instance.EndToEndFlowInstance;
import org.osate.aadl2.instance.FlowElementInstance;
import org.osate.aadl2.instance.FlowSpecificationInstance;
import org.osate.aadl2.util.OsateDebug;
import org.osate.analysis.flows.model.LatencyContributor;
import org.osate.analysis.flows.model.LatencyContributor.LatencyContributorMethod;
import org.osate.analysis.flows.model.LatencyContributorComponent;
import org.osate.analysis.flows.model.LatencyContributorConnection;
import org.osate.xtext.aadl2.properties.util.GetProperties;

public class FlowLatencyUtil {

	public static String getEndToEndFlowString(EndToEndFlowInstance etef) {
		StringBuffer ret;
		boolean firstPassed = false;

		ret = new StringBuffer();
		ret.append(etef.getName() + " : ");

		for (FlowElementInstance fei : etef.getFlowElements()) {
			if (firstPassed == true) {
				ret.append("->");
			}
			ret.append(fei.getName());
			firstPassed = true;
		}

		return ret.toString();
	}

	public static LatencyContributor mapFlowElementInstance(FlowElementInstance flowElementInstance) {

		OsateDebug.osateDebug("FlowLatencyUtil", "fei = " + flowElementInstance);
		OsateDebug.osateDebug("FlowLatencyUtil", "fei= name" + flowElementInstance.getName());

		if (flowElementInstance instanceof FlowSpecificationInstance) {
			FlowSpecificationInstance flowSpecification;
			ComponentInstance componentInstance;

			flowSpecification = (FlowSpecificationInstance) flowElementInstance;
			componentInstance = (ComponentInstance) flowElementInstance.getComponentInstance();
			return mapComponentInstance(flowElementInstance);
		}

		if (flowElementInstance instanceof ConnectionInstance) {
			return mapConnectionInstance(flowElementInstance);
		}

		return null;
	}

	public static LatencyContributorComponent mapComponentInstance(FlowElementInstance flowElementInstance) {
		LatencyContributorComponent latencyContributor;
		FlowSpecificationInstance flowSpecification;
		ComponentInstance componentInstance;
		LatencyContributorMethod method;

		double period;
		double deadline;
		double executionTimeLower;
		double executionTimeHigher;
		double worstCaseValue;
		double bestCaseValue;
		double expectedMin;
		double expectedMax;

		period = 0.0;
		deadline = 0.0;
		executionTimeHigher = 0.0;
		executionTimeLower = 0.0;
		worstCaseValue = 0.0;
		bestCaseValue = 0.0;
		expectedMax = 0.0;
		expectedMin = 0.0;

		flowSpecification = (FlowSpecificationInstance) flowElementInstance;
		componentInstance = (ComponentInstance) flowElementInstance.getComponentInstance();

		latencyContributor = new LatencyContributorComponent(componentInstance);

		expectedMin = GetProperties.getMinimumLatencyinMilliSec(flowElementInstance);
		expectedMax = GetProperties.getMaximumLatencyinMilliSec(flowElementInstance);

		/**
		 * Get all the relevant properties.
		 */
		period = GetProperties.getPeriodinMS(componentInstance);
		deadline = GetProperties.getDeadlineinMilliSec(componentInstance);
		executionTimeLower = GetProperties.getMinimumComputeExecutionTimeinMs(componentInstance);
		executionTimeHigher = GetProperties.getMaximumComputeExecutionTimeinMs(componentInstance);

		/**
		 * Selection of the worst case value;
		 */
		method = LatencyContributorMethod.UNKNOWN;

		if (executionTimeHigher != 0.0) {
			worstCaseValue = executionTimeHigher;
			method = LatencyContributorMethod.WCET;
		}

		if ((worstCaseValue == 0.0) && (deadline != 0.0)) {
			worstCaseValue = deadline;
			method = LatencyContributorMethod.DEADLINE;
		}

		if ((worstCaseValue == 0.0) && (period != 0.0)) {
			worstCaseValue = period;
			method = LatencyContributorMethod.PERIOD;
		}

		latencyContributor.setWorstCaseMethod(method);

		/**
		 * Selection of the best case value;
		 */
		method = LatencyContributorMethod.UNKNOWN;
		if (executionTimeLower != 0.0) {
			bestCaseValue = executionTimeLower;
			method = LatencyContributorMethod.WCET;
		}

		if ((bestCaseValue == 0.0) && (deadline != 0.0)) {
			bestCaseValue = deadline;
			method = LatencyContributorMethod.DEADLINE;
		}

		if ((bestCaseValue == 0.0) && (period != 0.0)) {
			bestCaseValue = period;
			method = LatencyContributorMethod.PERIOD;
		}

		latencyContributor.setBestCaseMethod(method);

		latencyContributor.setMaximum(worstCaseValue);
		latencyContributor.setMinimum(bestCaseValue);
		latencyContributor.setExpectedMaximum(expectedMax);
		latencyContributor.setExpectedMinimum(expectedMin);

		OsateDebug.osateDebug("FlowLatencyUtil", "flowSpecification component=" + componentInstance.getName());

		return latencyContributor;
	}

	public static LatencyContributor mapConnectionInstance(FlowElementInstance flowElementInstance) {
		LatencyContributor latencyContributor;
		ConnectionInstance connectionInstance;
		ComponentInstance boundBus;

		connectionInstance = (ConnectionInstance) flowElementInstance;
		latencyContributor = new LatencyContributorConnection(connectionInstance);

		OsateDebug.osateDebug("FlowLatencyUtil", "flowSpecification connection=" + connectionInstance);

		boundBus = GetProperties.getBoundBus(connectionInstance);

		if (boundBus != null) {
			OsateDebug.osateDebug("FlowLatencyUtil", "connection bound to bus=" + boundBus);
		}

		return latencyContributor;
	}
}
