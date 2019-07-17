package quack;

import java.util.Set;

import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IResourceDeltaVisitor;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.ITypeBinding;

import MyUtil.SyncCache;

public class ModelCache implements Runnable {

    public SyncCache<ICompilationUnit, Model> cache;

    public ModelCache(int n) {
        cache = new SyncCache(n);
        new Thread(this).start();
        ResourcesPlugin.getWorkspace().addResourceChangeListener(
                new IResourceChangeListener() {
                    public void resourceChanged(IResourceChangeEvent event) {
                        try {
                            event.getDelta().accept(
                                    new IResourceDeltaVisitor() {

                                        public boolean visit(
                                                IResourceDelta delta)
                                                throws CoreException {
                                            try {
                                                onChange(delta);
                                            } catch (Exception e) {
                                                Main.getMain().log("ModelCache.java(inner loop at top)", e);
                                            }
                                            return true;
                                        }

                                    });
                        } catch (Exception e) {
                            Main.getMain().log("ModelCache.java(outter loop at top)", e);
                        }
                    }
                });
    }

    public void onChange(IResourceDelta delta) {
        IJavaElement e = JavaCore.create(delta.getResource());
        if (e == null)
            return;
        if (!(e instanceof ICompilationUnit))
            return;
        if ((delta.getFlags() & IResourceDelta.CONTENT) == 0)
            return;

        ICompilationUnit unit = (ICompilationUnit) e;

        CompilationUnit ast = EclipseUtil.compile(unit);
        Set<ITypeBinding> types = EclipseUtil
                .getDeclaredTypesIncludingMethodClasses(ast);

        for (Model model : cache.getValues()) {
            synchronized (model) {
                model.updateTypes(types);
            }
        }
    }

    public Model getModel(ICompilationUnit unit, CompilationUnit ast)
            throws Exception {

        Model model = cache.get(unit);
        if (model == null) {
            cache.makeRoom();
            model = new Model(ast);
            cache.put(unit, model);
        }
        cache.touch(unit);
        return model;
    }

    public void run() {
        while (true) {
            try {
                for (ICompilationUnit unit : cache.getKeys()) {
                    ASTParser p = ASTParser.newParser(AST.JLS3);
                    p.setResolveBindings(true);
                    p.setStatementsRecovery(true);
                    p.setSource(unit);
                    CompilationUnit ast = EclipseUtil.compile(unit);
                    Model model = new Model(ast);
                    cache.replace(unit, model);

                    Thread.sleep(60 * 1000);
                }
            } catch (Throwable t) {
                Main.getMain().log("ModelCache.java(worker thread)", t);
            }
            try {
                Thread.sleep(60 * 1000);
            } catch (Exception e) {
            }
        }
    }
}
