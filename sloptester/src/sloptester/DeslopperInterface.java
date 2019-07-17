package sloptester;

import java.util.List;
import java.util.Set;

import org.eclipse.jdt.core.CompletionProposal;
import org.eclipse.jdt.core.dom.IPackageBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;

import MyUtil.MyNode;

public interface DeslopperInterface {
    public String deslop(String slop, Type desiredType, Tomb tomb, int localStartIndex,
    		double[] bestScore) throws Exception;
    public double getScoreFor(MyNode<Func> node);
    public int getIterations();
}
