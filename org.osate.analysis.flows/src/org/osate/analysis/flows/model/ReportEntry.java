package org.osate.analysis.flows.model;

import java.util.ArrayList;
import java.util.List;

import org.osate.aadl2.instance.EndToEndFlowInstance;
import org.osate.analysis.flows.reporting.model.Line;
import org.osate.analysis.flows.reporting.model.Section;

/*
 * A report entry corresponds to the entry within the report.
 * It just contains all the contributors for a flow latency.
 * We should have one report entry for each end to end flow
 */

public class ReportEntry {

	List<LatencyContributor> contributors;
	EndToEndFlowInstance relatedEndToEndFlow;

	public ReportEntry() {
		this.contributors = new ArrayList<LatencyContributor>();
	}

	public ReportEntry(EndToEndFlowInstance etef) {
		this();
		this.relatedEndToEndFlow = etef;
	}

	public void addContributor(LatencyContributor lc) {
		this.contributors.add(lc);
	}

	public List<LatencyContributor> getContributors() {
		return this.contributors;
	}

	public double getMinimumLatency() {
		double res = 0.0;
		for (LatencyContributor lc : contributors) {
			res = res + lc.getMinimum();
		}

		return res;
	}

	public double getMaximumLatency() {
		double res = 0.0;
		for (LatencyContributor lc : contributors) {
			res = res + lc.getMaximum();
		}

		return res;
	}

	public Section export() {
		Section section;

		section = new Section();
		for (LatencyContributor lc : this.contributors) {
			for (Line l : lc.export()) {
				section.addLine(l);
			}
		}
		return section;
	}

}