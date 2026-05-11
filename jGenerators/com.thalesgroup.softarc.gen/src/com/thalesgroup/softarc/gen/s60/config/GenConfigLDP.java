package com.thalesgroup.softarc.gen.s60.config;

import com.thalesgroup.softarc.gen.common.AbstractPass;
import com.thalesgroup.softarc.sf.Executable;
import com.thalesgroup.softarc.sf.ExternChannel;
import com.thalesgroup.softarc.sf.impl.QExternChannel;
import technology.ecoa.model.deployment.IOInterface;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;


public class GenConfigLDP extends AbstractPass {
    @Override
    public void execute() throws Exception {
        createChannels(context.system.getMapping().getGlobalExecutable());
    }

    private void createChannels(Executable globalExecutable){

        Set<IOInterface> alreadyAddedInOutChannels = new HashSet<IOInterface>();
        if (context.DEFILE.getExternalIo() != null) {
            for (IOInterface inOut : context.DEFILE.getExternalIo().getInOutPort()) {
                globalExecutable.setHasChannels(true);
                ExternChannel channel = new QExternChannel();
                channel.setName(inOut.getName());
                channel.setDirectionIn(true);
                channel.setDirectionOut(true);
                globalExecutable.getChannels().add(channel);
                alreadyAddedInOutChannels.add(inOut);
            }

            for (IOInterface outInterface : context.DEFILE.getExternalIo().getOutPort()) {
                if(!alreadyAddedInOutChannels.contains(outInterface)) {
                    globalExecutable.setHasChannels(true);
                    ExternChannel channel = new QExternChannel();
                    channel.setName(outInterface.getName());
                    channel.setDirectionIn(false);
                    channel.setDirectionOut(true);
                    globalExecutable.getChannels().add(channel);
                }

            }

            for (IOInterface inInterface : context.DEFILE.getExternalIo().getInPort()) {
                if(!alreadyAddedInOutChannels.contains(inInterface)) {
                    globalExecutable.setHasChannels(true);
                    ExternChannel channel = new QExternChannel();
                    channel.setName(inInterface.getName());
                    channel.setDirectionIn(true);
                    channel.setDirectionOut(false);
                    globalExecutable.getChannels().add(channel);
                }
            }
        }
    }
}
