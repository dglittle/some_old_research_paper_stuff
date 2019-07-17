package quack;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.Assert;
import org.eclipse.jdt.ui.text.java.IJavaCompletionProposal;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.jface.text.contentassist.IContextInformation;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;

public class MyCompletionProposal implements ICompletionProposal, IJavaCompletionProposal {

    private IProject project;
    
    private String fDisplayString;

    private String fReplacementString;

    private int fReplacementOffset;

    private int fReplacementLength;

    private int fCursorPosition;

    private Image fImage;

    private IContextInformation fContextInformation;

    private String fAdditionalProposalInfo;

    private int fRelevance;

    public MyCompletionProposal(IProject proj, String replacementString,
            int replacementOffset, int replacementLength, int cursorPosition,
            int relevance) {
        this(proj, replacementString, replacementOffset, replacementLength,
                cursorPosition, null, null, null, null, relevance);
    }

    public MyCompletionProposal(IProject proj, String replacementString,
            int replacementOffset, int replacementLength, int cursorPosition,
            Image image, String displayString,
            IContextInformation contextInformation,
            String additionalProposalInfo, int relevance) {
        Assert.isNotNull(replacementString);
        Assert.isTrue(replacementOffset >= 0);
        Assert.isTrue(replacementLength >= 0);
        Assert.isTrue(cursorPosition >= 0);

        project = proj;
        fReplacementString = replacementString;
        fReplacementOffset = replacementOffset;
        fReplacementLength = replacementLength;
        fCursorPosition = cursorPosition;
        fImage = image;
        fDisplayString = displayString;
        fContextInformation = contextInformation;
        fAdditionalProposalInfo = additionalProposalInfo;
        fRelevance = relevance;
        
        // log
        Main.getMain().log("guess: " + fReplacementString);
    }
    
    public String toString() {
    	return fReplacementString;
    }

    public void apply(IDocument document) {
        try {
            Main.getMain().log("chosen: " + fReplacementString);
            
            document.replace(fReplacementOffset, fReplacementLength,
                    fReplacementString);
        } catch (BadLocationException x) {
            // ignore
        }
    }

    public Point getSelection(IDocument document) {
        return new Point(fReplacementOffset + fCursorPosition, 0);
    }

    public IContextInformation getContextInformation() {
        return fContextInformation;
    }

    public Image getImage() {
        return fImage;
    }

    public String getDisplayString() {
        if (fDisplayString != null)
            return fDisplayString;
        return fReplacementString;
    }

    public String getAdditionalProposalInfo() {
        return fAdditionalProposalInfo;
    }

    public int getRelevance() {
        return fRelevance;
    }
}
