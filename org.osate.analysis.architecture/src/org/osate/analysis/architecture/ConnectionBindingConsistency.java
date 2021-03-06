/*
 * Created on July 7, 2004
 *
 * <copyright>
 * Copyright  2004 by Carnegie Mellon University, all rights reserved.
 *
 * Use of the Open Source AADL Tool Environment (OSATE) is subject to the terms of the license set forth
 * at http://www.eclipse.org/legal/cpl-v10.html.
 *
 * NO WARRANTY
 *
 * ANY INFORMATION, MATERIALS, SERVICES, INTELLECTUAL PROPERTY OR OTHER PROPERTY OR RIGHTS GRANTED OR PROVIDED BY
 * CARNEGIE MELLON UNIVERSITY PURSUANT TO THIS LICENSE (HEREINAFTER THE "DELIVERABLES") ARE ON AN "AS-IS" BASIS.
 * CARNEGIE MELLON UNIVERSITY MAKES NO WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED AS TO ANY MATTER INCLUDING,
 * BUT NOT LIMITED TO, WARRANTY OF FITNESS FOR A PARTICULAR PURPOSE, MERCHANTABILITY, INFORMATIONAL CONTENT,
 * NONINFRINGEMENT, OR ERROR-FREE OPERATION. CARNEGIE MELLON UNIVERSITY SHALL NOT BE LIABLE FOR INDIRECT, SPECIAL OR
 * CONSEQUENTIAL DAMAGES, SUCH AS LOSS OF PROFITS OR INABILITY TO USE SAID INTELLECTUAL PROPERTY, UNDER THIS LICENSE,
 * REGARDLESS OF WHETHER SUCH PARTY WAS AWARE OF THE POSSIBILITY OF SUCH DAMAGES. LICENSEE AGREES THAT IT WILL NOT
 * MAKE ANY WARRANTY ON BEHALF OF CARNEGIE MELLON UNIVERSITY, EXPRESS OR IMPLIED, TO ANY PERSON CONCERNING THE
 * APPLICATION OF OR THE RESULTS TO BE OBTAINED WITH THE DELIVERABLES UNDER THIS LICENSE.
 *
 * Licensee hereby agrees to defend, indemnify, and hold harmless Carnegie Mellon University, its trustees, officers,
 * employees, and agents from all claims or demands made against them (and any related losses, expenses, or
 * attorney's fees) arising out of, or relating to Licensee's and/or its sub licensees' negligent use or willful
 * misuse of or negligent conduct or willful misconduct regarding the Software, facilities, or other rights or
 * assistance granted by Carnegie Mellon University under this License, including, but not limited to, any claims of
 * product liability, personal injury, death, damage to property, or violation of any laws or regulations.
 *
 * Carnegie Mellon University Software Engineering Institute authored documents are sponsored by the U.S. Department
 * of Defense under Contract F19628-00-C-0003. Carnegie Mellon University retains copyrights in all material produced
 * under this contract. The U.S. Government retains a non-exclusive, royalty-free license to publish or reproduce these
 * documents, or allow others to do so, for U.S. Government purposes only pursuant to the copyright license
 * under the contract clause at 252.227.7013.
 *
 * </copyright>
 *
 *
 * @version $Id: ConnectionBindingConsistency.java,v 1.1.2.4 2008-12-01 13:23:35 relief Exp $
 */
package org.osate.analysis.architecture;

import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.emf.common.util.EList;
import org.osate.aadl2.instance.ComponentInstance;
import org.osate.aadl2.instance.ConnectionInstance;
import org.osate.aadl2.instance.ConnectionKind;
import org.osate.aadl2.instance.FeatureInstance;
import org.osate.aadl2.instance.util.InstanceSwitch;
import org.osate.aadl2.modelsupport.modeltraversal.AadlProcessingSwitchWithProgress;
import org.osate.ui.actions.AbstractAaxlAction;
import org.osate.xtext.aadl2.properties.util.InstanceModelUtil;

/**
 * @author phf
 *
 * This class provides for checking whether delta streams are communicated over protocols with guaranteed delivery.
 *
 */
public class ConnectionBindingConsistency extends AadlProcessingSwitchWithProgress {

	private AbstractAaxlAction action;

	public ConnectionBindingConsistency(final IProgressMonitor pm, final AbstractAaxlAction action) {
		super(pm, PROCESS_PRE_ORDER_ALL);
		this.action = action;
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	public final void initSwitches() {
		/* here we are creating the connection checking switches */

		/* here we are creating the connection checking switches */
		instanceSwitch = new InstanceSwitch() {
			/**
			 * check physical connectivity of port connection instances
			 */
			public Object caseConnectionInstance(ConnectionInstance conni) {
				if (conni.getKind().equals(ConnectionKind.PORT_CONNECTION)) {
					ComponentInstance srcHW = InstanceModelUtil
							.getHardwareComponent((FeatureInstance) conni.getSource());
					ComponentInstance dstHW = InstanceModelUtil
							.getHardwareComponent((FeatureInstance) conni.getDestination());
					if (srcHW == null || dstHW == null) {
						action.warning(conni, "Connection " + conni.getComponentInstancePath()
								+ " source or destination is not bound to hardware");
					}
					List<ComponentInstance> bindings = InstanceModelUtil.getPhysicalConnectionBinding(conni);
					if (bindings.isEmpty()) {
						action.warning(conni, "Connection " + conni.getComponentInstancePath()
								+ " has no actual connection binding to hardware");
						List<ComponentInstance> result = InstanceModelUtil.connectedByBus(srcHW, dstHW);
						if (result.isEmpty()) {
							action.error(conni,
									"Hardware (processor or device) of connection " + conni.getComponentInstancePath()
											+ " source and destination are not physically connected");
						}
					} else {
						ComponentInstance ci = bindings.get(0);
						if (srcHW != null && !InstanceModelUtil.connectedToBus(srcHW, ci)) {
							action.warning(conni, "Connection " + conni.getComponentInstancePath()
									+ " source bound hardware is not connected to the first bus in the actual binding");
						}
						ci = bindings.get(bindings.size() - 1);
						if (dstHW != null && !InstanceModelUtil.connectedToBus(srcHW, ci)) {
							action.warning(conni, "Connection " + conni.getComponentInstancePath()
									+ " destination bound hardware is not connected to the last bus in the actual binding");
						}
					}
				}
				return DONE;
			}
		};
	}

}
