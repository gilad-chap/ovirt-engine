package org.ovirt.engine.core.bll.network;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.ovirt.engine.core.common.action.SetupNetworksParameters;
import org.ovirt.engine.core.common.businessentities.Entities;
import org.ovirt.engine.core.common.businessentities.network.Network;
import org.ovirt.engine.core.common.businessentities.network.VdsNetworkInterface;
import org.ovirt.engine.core.common.errors.VdcBLLException;
import org.ovirt.engine.core.common.errors.VdcBllErrors;

public class AddNetworksByLabelParametersBuilder extends NetworkParametersBuilder {

    /**
     * Adds a list of labeled networks to a given interface
     */
    public SetupNetworksParameters buildParameters(VdsNetworkInterface nic,
            String label,
            List<Network> labeledNetworks) {
        SetupNetworksParameters parameters = createSetupNetworksParameters(nic.getVdsId());
        Set<Network> networkToAdd = getNetworksToConfigure(parameters.getInterfaces(), labeledNetworks);
        VdsNetworkInterface nicToConfigure = getNicToConfigure(parameters.getInterfaces(), nic.getId());
        if (nicToConfigure == null) {
            throw new VdcBLLException(VdcBllErrors.LABELED_NETWORK_INTERFACE_NOT_FOUND);
        }

        // add label to nic to be passed to setup-networks
        labelConfiguredNic(label, nicToConfigure);

        // configure networks on the nic
        parameters.getInterfaces().addAll(configureNetworks(nicToConfigure, networkToAdd));
        return parameters;
    }

    public void labelConfiguredNic(String label, VdsNetworkInterface nicToConfigure) {
        if (nicToConfigure.getLabels() == null) {
            nicToConfigure.setLabels(new HashSet<String>());
        }

        nicToConfigure.getLabels().add(label);
    }

    public Set<Network> getNetworksToConfigure(List<VdsNetworkInterface> nics, List<Network> labeledNetworks) {
        Map<String, VdsNetworkInterface> nicsByNetworkName = Entities.hostInterfacesByNetworkName(nics);
        Set<Network> networkToAdd = new HashSet<>();

        for (Network network : labeledNetworks) {
            if (!nicsByNetworkName.containsKey(network.getName())) {
                networkToAdd.add(network);
            }
        }

        return networkToAdd;
    }

    /**
     * Configure the networks on a specific nic and/or returns a list of vlans as new added interfaces configured
     * with vlan networks
     *
     * @param nic
     *            the underlying interface to configure
     * @param networks
     *            the networks to configure on the nic
     * @return a list of vlan devices or an empty list
     */
    public List<VdsNetworkInterface> configureNetworks(VdsNetworkInterface nic, Set<Network> networks) {
        List<VdsNetworkInterface> vlans = new ArrayList<>();
        for (Network network : networks) {
            configureNetwork(nic, vlans, network);
        }

        return vlans;
    }
}