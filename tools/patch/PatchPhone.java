import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

/**
 * Patch de MikasRevs Phone 2 (avec l'accord de son auteur) : dans les classes SERVEUR (server/ et MarketplaceManager), tout appel à
 * GameProfile.getName() devient fr.minenorth.phonepatch.NameHook.name(GameProfile) -> nom RP au lieu du pseudo Minecraft.
 * Même effet sur la pile (le GameProfile passe de receveur à argument, une String revient) : aucun autre changement de bytecode.
 * usage : java -cp asm.jar;. PatchPhone <jar d'origine> <jar patché> <NameHook.class> [CallHook.class [LivraisonHook.class livraison.png [GangHook.class gangs.png]]]
 */
public final class PatchPhone {
    static int total;

    static boolean included(String name) {
        return name.startsWith("com/mikasrevs/newphone/server/") || name.startsWith("com/mikasrevs/newphone/marketplace/MarketplaceManager");
    }

    public static void main(String[] a) throws Exception {
        byte[] hook = Files.readAllBytes(Path.of(a[2]));
        byte[] callHook = a.length > 3 ? Files.readAllBytes(Path.of(a[3])) : null;
        byte[] livraisonHook = a.length > 4 ? Files.readAllBytes(Path.of(a[4])) : null;
        byte[] livraisonIcon = a.length > 5 ? Files.readAllBytes(Path.of(a[5])) : null;
        byte[] gangHook = a.length > 6 ? Files.readAllBytes(Path.of(a[6])) : null;
        byte[] gangIcon = a.length > 7 ? Files.readAllBytes(Path.of(a[7])) : null;
        try (ZipFile in = new ZipFile(a[0]); ZipOutputStream out = new ZipOutputStream(Files.newOutputStream(Path.of(a[1])))) {
            for (Enumeration<? extends ZipEntry> e = in.entries(); e.hasMoreElements(); ) {
                ZipEntry en = e.nextElement();
                if (en.getName().startsWith("META-INF/") && (en.getName().endsWith(".SF") || en.getName().endsWith(".RSA") || en.getName().endsWith(".DSA"))) continue;
                byte[] data;
                try (InputStream is = in.getInputStream(en)) { data = is.readAllBytes(); }
                if (en.getName().equals(PatchCalls.CLASS)) {
                    data = PatchCalls.patch(data);
                    System.out.println("appels Police / Pompiers  " + en.getName() + "  (" + PatchCalls.patched + " points)");
                }
                if (en.getName().equals(PatchTabs.CLASS)) {
                    data = PatchTabs.patch(data);
                    System.out.println("onglets Police / Pompiers  " + en.getName());
                }
                if (livraisonHook != null && PatchLivraison.handles(en.getName())) {
                    data = PatchLivraison.patch(en.getName(), data);
                    System.out.println("application Livraison  " + en.getName());
                }
                if (!en.isDirectory() && en.getName().endsWith(".class") && included(en.getName())) {
                    int[] n = {0};
                    ClassReader cr = new ClassReader(data);
                    ClassWriter cw = new ClassWriter(cr, 0);
                    cr.accept(new ClassVisitor(Opcodes.ASM9, cw) {
                        @Override
                        public MethodVisitor visitMethod(int acc, String name, String desc, String sig, String[] ex) {
                            return new MethodVisitor(Opcodes.ASM9, super.visitMethod(acc, name, desc, sig, ex)) {
                                @Override
                                public void visitMethodInsn(int op, String owner, String mn, String md, boolean itf) {
                                    if (op == Opcodes.INVOKEVIRTUAL && owner.equals("com/mojang/authlib/GameProfile") && mn.equals("getName")
                                            && md.equals("()Ljava/lang/String;")) {
                                        n[0]++;
                                        super.visitMethodInsn(Opcodes.INVOKESTATIC, "fr/minenorth/phonepatch/NameHook", "name",
                                                "(Lcom/mojang/authlib/GameProfile;)Ljava/lang/String;", false);
                                    } else {
                                        super.visitMethodInsn(op, owner, mn, md, itf);
                                    }
                                }
                            };
                        }
                    }, 0);
                    if (n[0] > 0) {
                        System.out.println(n[0] + "  " + en.getName());
                        total += n[0];
                        data = cw.toByteArray();
                    }
                }
                ZipEntry ne = new ZipEntry(en.getName());
                ne.setTime(en.getTime());
                out.putNextEntry(ne);
                out.write(data);
                out.closeEntry();
            }
            ZipEntry he = new ZipEntry("fr/minenorth/phonepatch/NameHook.class");
            out.putNextEntry(he);
            out.write(hook);
            out.closeEntry();
            if (callHook != null) {
                ZipEntry ce = new ZipEntry("fr/minenorth/phonepatch/CallHook.class");
                out.putNextEntry(ce);
                out.write(callHook);
                out.closeEntry();
            }
            if (livraisonHook != null) {
                out.putNextEntry(new ZipEntry("fr/minenorth/phonepatch/LivraisonHook.class"));
                out.write(livraisonHook);
                out.closeEntry();
            }
            if (gangHook != null) {
                out.putNextEntry(new ZipEntry("fr/minenorth/phonepatch/GangHook.class"));
                out.write(gangHook);
                out.closeEntry();
            }
            if (gangIcon != null) {
                out.putNextEntry(new ZipEntry("assets/newphone/textures/gui/phone/apps/gangs.png"));
                out.write(gangIcon);
                out.closeEntry();
            }
            if (livraisonIcon != null) {
                out.putNextEntry(new ZipEntry("assets/newphone/textures/gui/phone/apps/livraison.png"));
                out.write(livraisonIcon);
                out.closeEntry();
            }
        }
        System.out.println("total : " + total + " appels remplacés");
    }
}
