import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import java.util.ArrayList;
import java.util.List;

/**
 * Application « Livraison » (commande de repas du mod MineNorth Livraison) sur l'écran d'accueil du téléphone. Quatre classes sont patchées :
 *  - PhoneApp (enum) : nouvelle constante LIVRAISON en dernière position (les ordinaux existants ne bougent pas), $values() et displayName() ;
 *  - MPhoneAppIconRenderer : icône raster textures/gui/phone/apps/livraison.png (64x64, ajoutée au jar par PatchPhone) ;
 *  - PhoneAppLayoutStore : DEFAULT_HOME passe par LivraisonHook.home (ajoute l'application seulement si le mod Livraison est présent) ;
 *  - ZPhoneClient.openSharedApp : le clic sur l'icône appelle LivraisonHook.open (commande /livraison commander via le mod Livraison).
 * Les joueurs qui ont déjà un agencement enregistré la reçoivent aussi : le téléphone y ajoute les applications par défaut qui manquent.
 */
final class PatchLivraison {
    static final String APP = "com/mikasrevs/newphone/client/gui/PhoneApp";
    static final String APP_CLASS = APP + ".class";
    static final String ICONS_CLASS = "com/mikasrevs/newphone/client/zphone/gui/MPhoneAppIconRenderer.class";
    static final String LAYOUT_CLASS = "com/mikasrevs/newphone/client/settings/PhoneAppLayoutStore.class";
    static final String CLIENT_CLASS = "com/mikasrevs/newphone/client/zphone/ZPhoneClient.class";
    static final String HOOK = "fr/minenorth/phonepatch/LivraisonHook";
    static final String APP_D = "L" + APP + ";";
    static final String RL = "net/minecraft/resources/ResourceLocation";
    static final int ORANGE = 0xFFFF7A1A;

    private PatchLivraison() {}

    static boolean handles(String entry) {
        return entry.equals(APP_CLASS) || entry.equals(ICONS_CLASS) || entry.equals(LAYOUT_CLASS) || entry.equals(CLIENT_CLASS);
    }

    static byte[] patch(String entry, byte[] data) {
        ClassReader cr = new ClassReader(data);
        // frames d'origine conservées (COMPUTE_FRAMES typerait Object les types Minecraft : VerifyError) ; frames ajoutées à la main
        ClassWriter cw = new ClassWriter(cr, ClassWriter.COMPUTE_MAXS);
        ClassVisitor cv = switch (entry) {
            case APP_CLASS -> app(cw, cr);
            case ICONS_CLASS -> icons(cw);
            case LAYOUT_CLASS -> layout(cw);
            default -> client(cw);
        };
        cr.accept(cv, 0);
        return cw.toByteArray();
    }

    // ------------------------------------------------------------------ PhoneApp

    static ClassVisitor app(ClassWriter cw, ClassReader cr) {
        List<String> constants = new ArrayList<>();
        cr.accept(new ClassVisitor(Opcodes.ASM9) {
            @Override public org.objectweb.asm.FieldVisitor visitField(int acc, String n, String d, String s, Object v) {
                if ((acc & Opcodes.ACC_ENUM) != 0) constants.add(n);
                return null;
            }
        }, ClassReader.SKIP_CODE);
        int ordinal = constants.size();
        return new ClassVisitor(Opcodes.ASM9, cw) {
            @Override
            public MethodVisitor visitMethod(int acc, String name, String desc, String sig, String[] ex) {
                if (name.equals("$values")) return null;   // remplacée dans visitEnd
                MethodVisitor mv = super.visitMethod(acc, name, desc, sig, ex);
                if (name.equals("<clinit>")) {
                    return new MethodVisitor(Opcodes.ASM9, mv) {
                        @Override public void visitMethodInsn(int op, String owner, String mn, String md, boolean itf) {
                            if (op == Opcodes.INVOKESTATIC && owner.equals(APP) && mn.equals("$values")) {
                                visitTypeInsn(Opcodes.NEW, APP);
                                visitInsn(Opcodes.DUP);
                                visitLdcInsn("LIVRAISON");
                                visitLdcInsn(ordinal);
                                visitLdcInsn("screen.newphone.app.livraison");
                                visitInsn(Opcodes.ICONST_M1);          // iconIndex : -1 = icône dessinée à part (MPhoneAppIconRenderer)
                                visitLdcInsn(ORANGE);
                                visitMethodInsn(Opcodes.INVOKESPECIAL, APP, "<init>", "(Ljava/lang/String;ILjava/lang/String;II)V", false);
                                visitFieldInsn(Opcodes.PUTSTATIC, APP, "LIVRAISON", APP_D);
                            }
                            super.visitMethodInsn(op, owner, mn, md, itf);
                        }
                    };
                }
                if (name.equals("displayName") && desc.equals("()Ljava/lang/String;")) {
                    return new MethodVisitor(Opcodes.ASM9, mv) {
                        @Override public void visitCode() {
                            super.visitCode();
                            Label other = new Label();
                            visitVarInsn(Opcodes.ALOAD, 0);
                            visitFieldInsn(Opcodes.GETSTATIC, APP, "LIVRAISON", APP_D);
                            visitJumpInsn(Opcodes.IF_ACMPNE, other);
                            visitLdcInsn("Livraison");
                            visitInsn(Opcodes.ARETURN);
                            visitLabel(other);
                            visitFrame(Opcodes.F_SAME, 0, null, 0, null);
                        }
                    };
                }
                return mv;
            }

            @Override
            public void visitEnd() {
                super.visitField(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC | Opcodes.ACC_FINAL | Opcodes.ACC_ENUM, "LIVRAISON", APP_D, null, null).visitEnd();
                MethodVisitor m = super.visitMethod(Opcodes.ACC_PRIVATE | Opcodes.ACC_STATIC | Opcodes.ACC_SYNTHETIC, "$values", "()[" + APP_D, null, null);
                m.visitCode();
                m.visitLdcInsn(ordinal + 1);
                m.visitTypeInsn(Opcodes.ANEWARRAY, APP);
                for (int i = 0; i < ordinal; i++) {
                    m.visitInsn(Opcodes.DUP);
                    m.visitLdcInsn(i);
                    m.visitFieldInsn(Opcodes.GETSTATIC, APP, constants.get(i), APP_D);
                    m.visitInsn(Opcodes.AASTORE);
                }
                m.visitInsn(Opcodes.DUP);
                m.visitLdcInsn(ordinal);
                m.visitFieldInsn(Opcodes.GETSTATIC, APP, "LIVRAISON", APP_D);
                m.visitInsn(Opcodes.AASTORE);
                m.visitInsn(Opcodes.ARETURN);
                m.visitMaxs(0, 0);
                m.visitEnd();
                super.visitEnd();
            }
        };
    }

    // ------------------------------------------------------------------ MPhoneAppIconRenderer

    static ClassVisitor icons(ClassWriter cw) {
        String self = "com/mikasrevs/newphone/client/zphone/gui/MPhoneAppIconRenderer";
        return new ClassVisitor(Opcodes.ASM9, cw) {
            @Override
            public MethodVisitor visitMethod(int acc, String name, String desc, String sig, String[] ex) {
                MethodVisitor mv = super.visitMethod(acc, name, desc, sig, ex);
                if (name.equals("<clinit>")) {
                    return new MethodVisitor(Opcodes.ASM9, mv) {
                        @Override public void visitFieldInsn(int op, String owner, String fn, String fd) {
                            if (op == Opcodes.PUTSTATIC && fn.equals("RASTER_ICONS"))
                                visitMethodInsn(Opcodes.INVOKESTATIC, self, "mnIcons", "(Ljava/util/Map;)Ljava/util/Map;", false);
                            super.visitFieldInsn(op, owner, fn, fd);
                        }
                    };
                }
                return mv;
            }

            /** private static Map mnIcons(Map m) : copie modifiable + LIVRAISON -> rasterIcon("livraison"). */
            @Override
            public void visitEnd() {
                MethodVisitor m = super.visitMethod(Opcodes.ACC_PRIVATE | Opcodes.ACC_STATIC, "mnIcons", "(Ljava/util/Map;)Ljava/util/Map;", null, null);
                m.visitCode();
                m.visitTypeInsn(Opcodes.NEW, "java/util/HashMap");
                m.visitInsn(Opcodes.DUP);
                m.visitVarInsn(Opcodes.ALOAD, 0);
                m.visitMethodInsn(Opcodes.INVOKESPECIAL, "java/util/HashMap", "<init>", "(Ljava/util/Map;)V", false);
                m.visitVarInsn(Opcodes.ASTORE, 1);
                m.visitVarInsn(Opcodes.ALOAD, 1);
                m.visitFieldInsn(Opcodes.GETSTATIC, APP, "LIVRAISON", APP_D);
                m.visitLdcInsn("livraison");
                m.visitMethodInsn(Opcodes.INVOKESTATIC, self, "rasterIcon", "(Ljava/lang/String;)L" + RL + ";", false);
                m.visitMethodInsn(Opcodes.INVOKEINTERFACE, "java/util/Map", "put", "(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;", true);
                m.visitInsn(Opcodes.POP);
                m.visitVarInsn(Opcodes.ALOAD, 1);
                m.visitInsn(Opcodes.ARETURN);
                m.visitMaxs(0, 0);
                m.visitEnd();
                super.visitEnd();
            }
        };
    }

    // ------------------------------------------------------------------ PhoneAppLayoutStore

    static ClassVisitor layout(ClassWriter cw) {
        return new ClassVisitor(Opcodes.ASM9, cw) {
            @Override
            public MethodVisitor visitMethod(int acc, String name, String desc, String sig, String[] ex) {
                MethodVisitor mv = super.visitMethod(acc, name, desc, sig, ex);
                if (name.equals("<clinit>")) {
                    return new MethodVisitor(Opcodes.ASM9, mv) {
                        @Override public void visitFieldInsn(int op, String owner, String fn, String fd) {
                            if (op == Opcodes.PUTSTATIC && fn.equals("DEFAULT_HOME"))
                                visitMethodInsn(Opcodes.INVOKESTATIC, HOOK, "home", "(Ljava/util/List;)Ljava/util/List;", false);
                            super.visitFieldInsn(op, owner, fn, fd);
                        }
                    };
                }
                return mv;
            }
        };
    }

    // ------------------------------------------------------------------ ZPhoneClient

    static ClassVisitor client(ClassWriter cw) {
        return new ClassVisitor(Opcodes.ASM9, cw) {
            @Override
            public MethodVisitor visitMethod(int acc, String name, String desc, String sig, String[] ex) {
                MethodVisitor mv = super.visitMethod(acc, name, desc, sig, ex);
                if (name.equals("openSharedApp") && desc.equals("(" + APP_D + "Lnet/minecraft/client/gui/screens/Screen;)V")) {
                    return new MethodVisitor(Opcodes.ASM9, mv) {
                        @Override public void visitCode() {
                            super.visitCode();
                            Label go = new Label();
                            visitVarInsn(Opcodes.ALOAD, 0);
                            visitMethodInsn(Opcodes.INVOKESTATIC, HOOK, "open", "(" + APP_D + ")Z", false);
                            visitJumpInsn(Opcodes.IFEQ, go);
                            visitInsn(Opcodes.RETURN);
                            visitLabel(go);
                            visitFrame(Opcodes.F_SAME, 0, null, 0, null);
                        }
                    };
                }
                return mv;
            }
        };
    }
}
