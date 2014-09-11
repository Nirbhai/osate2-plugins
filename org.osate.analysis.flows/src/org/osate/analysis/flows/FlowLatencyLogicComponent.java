package org.osate.analysis.flows;

import org.osate.aadl2.instance.ComponentInstance;
import org.osate.aadl2.instance.EndToEndFlowInstance;
import org.osate.aadl2.instance.FlowElementInstance;
import org.osate.aadl2.instance.FlowSpecificationInstance;
import org.osate.analysis.flows.model.LatencyContributor.LatencyContributorMethod;
import org.osate.analysis.flows.model.LatencyContributorComponent;
import org.osate.analysis.flows.model.LatencyReportEntry;
import org.osate.analysis.flows.preferences.Values;
import org.osate.xtext.aadl2.properties.util.GetProperties;

public class FlowLatencyLogicComponent {

	public static void mapComponentInstance(final EndToEndFlowInstance etef,
			final FlowElementInstance flowElementInstance, LatencyReportEntry entry) {
		ComponentInstance componentInstance;
		double expectedMin = 0.0;
		double expectedMax = 0.0;

		if (flowElementInstance instanceof FlowSpecificationInstance) {
			componentInstance = flowElementInstance.getComponentInstance();
			expectedMin = GetProperties.getMinimumLatencyinMilliSec(flowElementInstance);
			expectedMax = GetProperties.getMaximumLatencyinMilliSec(flowElementInstance);
		} else {
			componentInstance = (ComponentInstance) flowElementInstance;
		}

		/**
		 * Get all the relevant properties.
		 */
		double period = GetProperties.getPeriodinMS(componentInstance);
		double deadline = GetProperties.getDeadlineinMilliSec(componentInstance);
		boolean isAssignedDeadline = GetProperties.isAsignedDeadline(componentInstance);
		double executionTimeLower = GetProperties.getMinimumComputeExecutionTimeinMs(componentInstance);
		double executionTimeHigher = GetProperties.getMaximumComputeExecutionTimeinMs(componentInstance);

		/**
		 * The component is periodic. Therefore it will sample its input unless we have an immediate connection or delayed connection
		 */
		boolean checkLastImmediate = false;
		if (period > 0) {
			LatencyContributorComponent samplingLatencyContributor = new LatencyContributorComponent(componentInstance);
			samplingLatencyContributor.setSamplingPeriod(period);
			if (FlowLatencyUtil.isPreviousConnectionDelayed(etef, flowElementInstance)) {
				samplingLatencyContributor.setBestCaseMethod(LatencyContributorMethod.DELAYED);
				samplingLatencyContributor.setWorstCaseMethod(LatencyContributorMethod.DELAYED);
			} else if (FlowLatencyUtil.isPreviousConnectionImmediate(etef, flowElementInstance)) {
				// we include this contributor so we can check for consistency for LAST_IMMEDIATE, i.e.,
				// the cumulative does not exceed the deadline of the last.
				if (!FlowLatencyUtil.isNextConnectionImmediate(etef, flowElementInstance)) {
					checkLastImmediate = true;
				}
			} else {
				if (entry.getContributors().isEmpty()) {
					samplingLatencyContributor.reportInfo("Initial " + period + "ms sampling latency not added");
					samplingLatencyContributor.setBestCaseMethod(LatencyContributorMethod.FIRST_SAMPLED);
					samplingLatencyContributor.setWorstCaseMethod(LatencyContributorMethod.FIRST_SAMPLED);
					// insert first partition sampling before task sampling. FOr other partitions it is inserted by connection processing
					ComponentInstance firstPartition = FlowLatencyUtil.getPartition(componentInstance);
					if (firstPartition != null) {
						double partitionLatency = FlowLatencyUtil.getPartitionLatency(firstPartition);
						double frameOffset = FlowLatencyUtil.getPartitionFrameOffset(firstPartition);
						if (frameOffset != -1) {
							LatencyContributorComponent partitionLatencyContributor = new LatencyContributorComponent(
									firstPartition);
							partitionLatencyContributor.setSamplingPeriod(partitionLatency);
							partitionLatencyContributor.setPartitionOffset(frameOffset);
							double partitionDuration = FlowLatencyUtil.getPartitionDuration(firstPartition);
							partitionLatencyContributor.setPartitionDuration(partitionDuration);
							partitionLatencyContributor.setWorstCaseMethod(LatencyContributorMethod.PARTITION_SCHEDULE);
							partitionLatencyContributor.setBestCaseMethod(LatencyContributorMethod.PARTITION_SCHEDULE);
							partitionLatencyContributor.reportInfo("Initial " + period
									+ "ms partition latency not added");
							entry.addContributor(partitionLatencyContributor);
						} else {
							LatencyContributorComponent partitionLatencyContributor = new LatencyContributorComponent(
									firstPartition);
							partitionLatencyContributor.setSamplingPeriod(partitionLatency);
							partitionLatencyContributor.setWorstCaseMethod(LatencyContributorMethod.PARTITION_FRAME);
							partitionLatencyContributor.setBestCaseMethod(LatencyContributorMethod.PARTITION_FRAME);
							partitionLatencyContributor.reportInfo("Initial " + period
									+ "ms partition latency not added");
							entry.addContributor(partitionLatencyContributor);
						}
					}
				} else {
					samplingLatencyContributor.setBestCaseMethod(LatencyContributorMethod.SAMPLED);
					samplingLatencyContributor.setWorstCaseMethod(LatencyContributorMethod.SAMPLED);
				}
			}
			entry.addContributor(samplingLatencyContributor);
		} else {
			// insert first partition sampling for the aperiodic case. For other partitions it is inserted by connection processing
			ComponentInstance firstPartition = FlowLatencyUtil.getPartition(componentInstance);
			if (firstPartition != null) {
				double partitionLatency = FlowLatencyUtil.getPartitionLatency(firstPartition);
				double frameOffset = FlowLatencyUtil.getPartitionFrameOffset(firstPartition);
				if (frameOffset != -1) {
					LatencyContributorComponent platencyContributor = new LatencyContributorComponent(firstPartition);
					platencyContributor.setSamplingPeriod(partitionLatency);
					platencyContributor.setPartitionOffset(frameOffset);
					platencyContributor.setWorstCaseMethod(LatencyContributorMethod.PARTITION_SCHEDULE);
					platencyContributor.setBestCaseMethod(LatencyContributorMethod.PARTITION_SCHEDULE);
					platencyContributor.reportInfo("Initial " + period + "ms partition latency not added");
					entry.addContributor(platencyContributor);
				} else {
					LatencyContributorComponent platencyContributor = new LatencyContributorComponent(firstPartition);
					platencyContributor.setSamplingPeriod(partitionLatency);
					platencyContributor.setWorstCaseMethod(LatencyContributorMethod.PARTITION_FRAME);
					platencyContributor.setBestCaseMethod(LatencyContributorMethod.PARTITION_FRAME);
					platencyContributor.reportInfo("Initial " + period + "ms partition latency not added");
					entry.addContributor(platencyContributor);
				}
			}
		}

		/**
		 * Selection of the worst case value, generic case.
		 */
		LatencyContributorMethod bestmethod;
		LatencyContributorMethod worstmethod;

		double worstCaseValue = 0.0;
		double bestCaseValue = 0.0;
		worstmethod = LatencyContributorMethod.UNKNOWN;

		if (executionTimeHigher != 0.0 && !Values.doWorstCaseDeadline()) {
			// Use deadline for worst-case if specified in preferences
			worstCaseValue = executionTimeHigher;
			worstmethod = LatencyContributorMethod.PROCESSING_TIME;
		}

		if ((worstCaseValue == 0.0) && isAssignedDeadline) {
			// filter out if deadline was not explicitly assigned
			worstCaseValue = deadline;
			worstmethod = LatencyContributorMethod.DEADLINE;
		}

		if ((worstCaseValue == 0.0) && (expectedMax != 0.0)) {
			worstCaseValue = expectedMax;
			worstmethod = LatencyContributorMethod.SPECIFIED;
		}

		/**
		 * Selection of the best case value, generic cases.
		 */
		bestmethod = LatencyContributorMethod.UNKNOWN;
		if (executionTimeLower != 0.0) {
			bestCaseValue = executionTimeLower;
			bestmethod = LatencyContributorMethod.PROCESSING_TIME;
		}
//
//		if ((bestCaseValue == 0.0) && isAssignedDeadline) {
//			bestCaseValue = deadline;
//			bestmethod = LatencyContributorMethod.DEADLINE;
//		}

		if ((bestCaseValue == 0.0) && (expectedMin != 0.0)) {
			bestCaseValue = expectedMin;
			bestmethod = LatencyContributorMethod.SPECIFIED;
		}

		LatencyContributorComponent processingLatencyContributor = new LatencyContributorComponent(componentInstance);
		processingLatencyContributor.setWorstCaseMethod(worstmethod);
		processingLatencyContributor.setBestCaseMethod(bestmethod);
		processingLatencyContributor.setMaximum(worstCaseValue);
		processingLatencyContributor.setMinimum(bestCaseValue);
		processingLatencyContributor.setExpectedMaximum(expectedMax);
		processingLatencyContributor.setExpectedMinimum(expectedMin);
		if (checkLastImmediate && deadline > 0.0) {

			processingLatencyContributor.setImmediateDeadline(deadline);
		}
		processingLatencyContributor.checkConsistency();
		entry.addContributor(processingLatencyContributor);
	}
}
