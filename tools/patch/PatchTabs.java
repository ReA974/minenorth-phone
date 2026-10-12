import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

/**
 * Écran « Urgences » du téléphone (accueil) : deux onglets Police / Pompiers, affichés seulement pour un joueur reconnu répondant
 * (EmergencyClientData.isAuthorized), qui ouvrent l'écran de la tablette de répondant (ResponderTabletScreen.open) sans l'objet tablette.
 * Trois méthodes privées sont ajoutées à ZPhoneEmergencyScreen (mnPill, mnTabs, mnClick) et branchées dans drawHome / m_6375_ / le constructeur.
 */
final class PatchTabs {
    static final String CLASS = "com/mikasrevs/newphone/client/zphone/gui/ZPhoneEmergencyScreen.class";
    static final String EM = "com/mikasrevs/newphone/client/zphone/gui/ZPhoneEmergencyScreen";
    static final String GFX = "net/minecraft/client/gui/GuiGraphics";
    static final String SVC = "com/mikasrevs/newphone/emergency/EmergencyService";
    static final String CLIENT = "com/mikasrevs/newphone/client/emergency/EmergencyClientData";
    static final String TABLET = "com/mikasrevs/newphone/client/tablet/ResponderTabletScreen";
    static final String DISPATCH = "com/mikasrevs/newphone/network/EmergencyStateData$DispatchEntry";
    static final String STEP = EM + "$Step";
    static final String PILL = "(L" + GFX + ";IIL" + SVC + ";IIILjava/lang/String;)V";
    static final float TOP = 503f, BOTTOM = 543f;

    private PatchTabs() {}

    static byte[] patch(byte[] data) {
        ClassReader cr = new ClassReader(data);
        ClassWriter cw = new ClassWriter(cr, ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS) {
            @Override protected String getCommonSuperClass(String a, String b) { return "java/lang/Object"; }
        };
        cr.accept(new ClassVisitor(Opcodes.ASM9, cw) {
            @Override
            public MethodVisitor visitMethod(int acc, String name, String desc, String sig, String[] ex) {
                MethodVisitor mv = super.visitMethod(acc, name, desc, sig, ex);
                if (name.equals("drawHome") && desc.equals("(L" + GFX + ";II)V")) {
                    return new MethodVisitor(Opcodes.ASM9, mv) {
                        @Override public void visitInsn(int op) {
                            if (op == Opcodes.RETURN) {
                                visitVarInsn(Opcodes.ALOAD, 0); visitVarInsn(Opcodes.ALOAD, 1);
                                visitVarInsn(Opcodes.ILOAD, 2); visitVarInsn(Opcodes.ILOAD, 3);
                                visitMethodInsn(Opcodes.INVOKESPECIAL, EM, "mnTabs", "(L" + GFX + ";II)V", false);
                            }
                            super.visitInsn(op);
                        }
                    };
                }
                if (name.equals("m_6375_") && desc.equals("(DDI)Z")) {
                    return new MethodVisitor(Opcodes.ASM9, mv) {
                        @Override public void visitCode() {
                            super.visitCode();
                            Label go = new Label();
                            visitVarInsn(Opcodes.ALOAD, 0); visitVarInsn(Opcodes.DLOAD, 1); visitVarInsn(Opcodes.DLOAD, 3);
                            visitVarInsn(Opcodes.ILOAD, 5);
                            visitMethodInsn(Opcodes.INVOKESPECIAL, EM, "mnClick", "(DDI)Z", false);
                            visitJumpInsn(Opcodes.IFEQ, go);
                            visitInsn(Opcodes.ICONST_1); visitInsn(Opcodes.IRETURN);
                            visitLabel(go);
                        }
                    };
                }
                if (name.equals("<init>")) {   // demande l'état des répondants au serveur : isAuthorized en dépend
                    return new MethodVisitor(Opcodes.ASM9, mv) {
                        @Override public void visitInsn(int op) {
                            if (op == Opcodes.RETURN) visitMethodInsn(Opcodes.INVOKESTATIC, CLIENT, "requestSync", "()V", false);
                            super.visitInsn(op);
                        }
                    };
                }
                return mv;
            }

            @Override
            public void visitEnd() {
                pillMethod();
                MethodVisitor m = super.visitMethod(Opcodes.ACC_PRIVATE, "mnTabs", "(L" + GFX + ";II)V", null, null);
                m.visitCode();
                pill(m, "POLICE", 22, 146, -14393701, "Police");
                pill(m, "EMS", 154, 278, -4834747, "Pompiers");
                m.visitInsn(Opcodes.RETURN);
                m.visitMaxs(0, 0);
                m.visitEnd();

                m = super.visitMethod(Opcodes.ACC_PRIVATE, "mnClick", "(DDI)Z", null, null);
                m.visitCode();
                Label no = new Label();
                m.visitMethodInsn(Opcodes.INVOKESTATIC, CLIENT, "ownDispatch", "()L" + DISPATCH + ";", false);
                m.visitJumpInsn(Opcodes.IFNONNULL, no);
                m.visitVarInsn(Opcodes.ILOAD, 5); m.visitJumpInsn(Opcodes.IFNE, no);
                m.visitVarInsn(Opcodes.ALOAD, 0); m.visitFieldInsn(Opcodes.GETFIELD, EM, "step", "L" + STEP + ";");
                m.visitFieldInsn(Opcodes.GETSTATIC, STEP, "HOME", "L" + STEP + ";");
                m.visitJumpInsn(Opcodes.IF_ACMPNE, no);
                click(m, "POLICE", 22, 146);
                click(m, "EMS", 154, 278);
                m.visitLabel(no);
                m.visitInsn(Opcodes.ICONST_0); m.visitInsn(Opcodes.IRETURN);
                m.visitMaxs(0, 0);
                m.visitEnd();
                super.visitEnd();
            }

            /** private void mnPill(GuiGraphics g, int mx, int my, EmergencyService svc, int x1, int x2, int color, String label) */
            void pillMethod() {
                MethodVisitor m = super.visitMethod(Opcodes.ACC_PRIVATE, "mnPill", PILL, null, null);
                m.visitCode();
                Label end = new Label(), plain = new Label(), draw = new Label();
                m.visitMethodInsn(Opcodes.INVOKESTATIC, CLIENT, "ownDispatch", "()L" + DISPATCH + ";", false);
                m.visitJumpInsn(Opcodes.IFNONNULL, end);
                m.visitVarInsn(Opcodes.ALOAD, 4);
                m.visitMethodInsn(Opcodes.INVOKESTATIC, CLIENT, "isAuthorized", "(L" + SVC + ";)Z", false);
                m.visitJumpInsn(Opcodes.IFEQ, end);
                m.visitVarInsn(Opcodes.ALOAD, 0); m.visitVarInsn(Opcodes.ILOAD, 2); m.visitInsn(Opcodes.I2D);
                m.visitVarInsn(Opcodes.ILOAD, 3); m.visitInsn(Opcodes.I2D);
                m.visitVarInsn(Opcodes.ILOAD, 5); m.visitInsn(Opcodes.I2F); m.visitLdcInsn(TOP);
                m.visitVarInsn(Opcodes.ILOAD, 6); m.visitInsn(Opcodes.I2F); m.visitLdcInsn(BOTTOM);
                m.visitMethodInsn(Opcodes.INVOKEVIRTUAL, EM, "inside", "(DDFFFF)Z", false);
                m.visitJumpInsn(Opcodes.IFEQ, plain);
                m.visitVarInsn(Opcodes.ILOAD, 7); m.visitMethodInsn(Opcodes.INVOKESTATIC, EM, "lighten", "(I)I", false);
                m.visitJumpInsn(Opcodes.GOTO, draw);
                m.visitLabel(plain);
                m.visitVarInsn(Opcodes.ILOAD, 7);
                m.visitLabel(draw);
                m.visitVarInsn(Opcodes.ISTORE, 9);
                // drawRoundedRect(g, x(x1), y(TOP), x(x2), y(BOTTOM), size(12), couleur)
                m.visitVarInsn(Opcodes.ALOAD, 0); m.visitVarInsn(Opcodes.ALOAD, 1);
                m.visitVarInsn(Opcodes.ALOAD, 0); m.visitVarInsn(Opcodes.ILOAD, 5); m.visitInsn(Opcodes.I2F);
                m.visitMethodInsn(Opcodes.INVOKEVIRTUAL, EM, "x", "(F)I", false);
                m.visitVarInsn(Opcodes.ALOAD, 0); m.visitLdcInsn(TOP); m.visitMethodInsn(Opcodes.INVOKEVIRTUAL, EM, "y", "(F)I", false);
                m.visitVarInsn(Opcodes.ALOAD, 0); m.visitVarInsn(Opcodes.ILOAD, 6); m.visitInsn(Opcodes.I2F);
                m.visitMethodInsn(Opcodes.INVOKEVIRTUAL, EM, "x", "(F)I", false);
                m.visitVarInsn(Opcodes.ALOAD, 0); m.visitLdcInsn(BOTTOM); m.visitMethodInsn(Opcodes.INVOKEVIRTUAL, EM, "y", "(F)I", false);
                m.visitVarInsn(Opcodes.ALOAD, 0); m.visitLdcInsn(12f); m.visitMethodInsn(Opcodes.INVOKEVIRTUAL, EM, "size", "(F)I", false);
                m.visitVarInsn(Opcodes.ILOAD, 9);
                m.visitMethodInsn(Opcodes.INVOKEVIRTUAL, EM, "drawRoundedRect", "(L" + GFX + ";IIIIII)V", false);
                // libellé centré
                m.visitVarInsn(Opcodes.ALOAD, 1); m.visitVarInsn(Opcodes.ALOAD, 8);
                m.visitVarInsn(Opcodes.ALOAD, 0); m.visitVarInsn(Opcodes.ILOAD, 5); m.visitVarInsn(Opcodes.ILOAD, 6); m.visitInsn(Opcodes.IADD);
                m.visitInsn(Opcodes.ICONST_2); m.visitInsn(Opcodes.IDIV); m.visitInsn(Opcodes.I2F);
                m.visitMethodInsn(Opcodes.INVOKEVIRTUAL, EM, "x", "(F)I", false);
                m.visitVarInsn(Opcodes.ALOAD, 0); m.visitLdcInsn(518f); m.visitMethodInsn(Opcodes.INVOKEVIRTUAL, EM, "y", "(F)I", false);
                m.visitVarInsn(Opcodes.ALOAD, 0); m.visitLdcInsn(11f); m.visitMethodInsn(Opcodes.INVOKEVIRTUAL, EM, "size", "(F)I", false);
                m.visitInsn(Opcodes.ICONST_M1);
                m.visitMethodInsn(Opcodes.INVOKESTATIC, "com/mikasrevs/newphone/client/gui/PhoneModernText", "drawCenteredColored",
                        "(L" + GFX + ";Ljava/lang/String;IIII)V", false);
                m.visitLabel(end);
                m.visitInsn(Opcodes.RETURN);
                m.visitMaxs(0, 0);
                m.visitEnd();
            }

            void pill(MethodVisitor m, String svc, int x1, int x2, int color, String label) {
                m.visitVarInsn(Opcodes.ALOAD, 0); m.visitVarInsn(Opcodes.ALOAD, 1);
                m.visitVarInsn(Opcodes.ILOAD, 2); m.visitVarInsn(Opcodes.ILOAD, 3);
                m.visitFieldInsn(Opcodes.GETSTATIC, SVC, svc, "L" + SVC + ";");
                m.visitIntInsn(Opcodes.SIPUSH, x1); m.visitIntInsn(Opcodes.SIPUSH, x2); m.visitLdcInsn(color); m.visitLdcInsn(label);
                m.visitMethodInsn(Opcodes.INVOKESPECIAL, EM, "mnPill", PILL, false);
            }

            void click(MethodVisitor m, String svc, int x1, int x2) {
                Label next = new Label();
                m.visitFieldInsn(Opcodes.GETSTATIC, SVC, svc, "L" + SVC + ";");
                m.visitMethodInsn(Opcodes.INVOKESTATIC, CLIENT, "isAuthorized", "(L" + SVC + ";)Z", false);
                m.visitJumpInsn(Opcodes.IFEQ, next);
                m.visitVarInsn(Opcodes.ALOAD, 0); m.visitVarInsn(Opcodes.DLOAD, 1); m.visitVarInsn(Opcodes.DLOAD, 3);
                m.visitIntInsn(Opcodes.SIPUSH, x1); m.visitInsn(Opcodes.I2F); m.visitLdcInsn(TOP);
                m.visitIntInsn(Opcodes.SIPUSH, x2); m.visitInsn(Opcodes.I2F); m.visitLdcInsn(BOTTOM);
                m.visitMethodInsn(Opcodes.INVOKEVIRTUAL, EM, "inside", "(DDFFFF)Z", false);
                m.visitJumpInsn(Opcodes.IFEQ, next);
                m.visitFieldInsn(Opcodes.GETSTATIC, SVC, svc, "L" + SVC + ";");
                m.visitMethodInsn(Opcodes.INVOKESTATIC, TABLET, "open", "(L" + SVC + ";)V", false);
                m.visitInsn(Opcodes.ICONST_1); m.visitInsn(Opcodes.IRETURN);
                m.visitLabel(next);
            }
        }, 0);
        return cw.toByteArray();
    }
}
