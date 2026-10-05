package Foundations.RotaryCraft.Platform;

import net.neoforged.neoforge.transfer.energy.EnergyHandler;
import net.neoforged.neoforge.transfer.transaction.Transaction;

/** Transactional simulation and exact commit: rejection rolls back BOTH handlers. */
public final class EnergyTransfer {
    public static int transfer(EnergyHandler source,EnergyHandler target,int maximum){
        if(maximum<=0||source==target)return 0;
        try(var transaction=Transaction.openRoot()){
            int accepted;
            try(var probe=Transaction.open(transaction)){accepted=target.insert(source.extract(maximum,probe),probe);}
            if(accepted<=0)return 0;
            int extracted=source.extract(accepted,transaction);
            if(extracted!=accepted||target.insert(extracted,transaction)!=extracted)return 0;
            transaction.commit();return accepted;
        }
    }
    private EnergyTransfer(){}
}
