import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Handle;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

/**
 * PhoneServerService, deux ajouts (avec l'accord de l'auteur du téléphone) :
 *  - startCall(ServerPlayer, String, boolean) : juste avant la recherche du joueur appelé (une fois les contrôles de contact passés), la cible est
 *    remplacée par CallHook.redirect(appelant, cible) : le contact « Police » / « Pompiers » devient le policier / secouriste qui répond.
 *  - addPlayerEntry(...) : le drapeau « en ligne » de l'entrée passe par CallHook.online(drapeau, uuid), pour que ces contacts virtuels aient un bouton d'appel.
 */
final class PatchCalls {
    static final String CLASS = "com/mikasrevs/newphone/server/PhoneServerService.class";
    static final String HOOK = "fr/minenorth/phonepatch/CallHook";
    static int patched;

    private PatchCalls() {}

    static byte[] patch(byte[] data) {
        ClassReader cr = new ClassReader(data);
        ClassWriter cw = new ClassWriter(cr, ClassWriter.COMPUTE_MAXS);
        cr.accept(new ClassVisitor(Opcodes.ASM9, cw) {
            @Override
            public MethodVisitor visitMethod(int acc, String name, String desc, String sig, String[] ex) {
                MethodVisitor mv = super.visitMethod(acc, name, desc, sig, ex);
                if (name.equals("startCall") && desc.equals("(Lnet/minecraft/server/level/ServerPlayer;Ljava/lang/String;Z)V")) return new StartCall(mv);
                if (name.equals("addPlayerEntry")) return new AddEntry(mv);
                return mv;
            }
        }, 0);
        return cw.toByteArray();
    }

    /** Insère « cible = CallHook.redirect(appelant, cible) » avant le premier « aload 3 ; getPlayerList » de startCall (locals : 4 = appelant, 5 = cible). */
    static final class StartCall extends MethodVisitor {
        private boolean pending, done;

        StartCall(MethodVisitor mv) { super(Opcodes.ASM9, mv); }

        private void flush() {
            if (pending) { pending = false; super.visitVarInsn(Opcodes.ALOAD, 3); }
        }

        @Override public void visitVarInsn(int op, int var) {
            flush();
            if (!done && op == Opcodes.ALOAD && var == 3) { pending = true; return; }
            super.visitVarInsn(op, var);
        }

        @Override public void visitMethodInsn(int op, String owner, String name, String desc, boolean itf) {
            if (pending && op == Opcodes.INVOKEVIRTUAL && owner.equals("net/minecraft/server/MinecraftServer") && name.equals("m_6846_")) {
                pending = false;
                done = true;
                patched++;
                super.visitVarInsn(Opcodes.ALOAD, 4);
                super.visitVarInsn(Opcodes.ALOAD, 5);
                super.visitMethodInsn(Opcodes.INVOKESTATIC, HOOK, "redirect", "(Ljava/util/UUID;Ljava/util/UUID;)Ljava/util/UUID;", false);
                super.visitVarInsn(Opcodes.ASTORE, 5);
                super.visitVarInsn(Opcodes.ALOAD, 3);
                super.visitMethodInsn(op, owner, name, desc, itf);
                return;
            }
            flush();
            super.visitMethodInsn(op, owner, name, desc, itf);
        }

        @Override public void visitInsn(int op) { flush(); super.visitInsn(op); }
        @Override public void visitJumpInsn(int op, Label l) { flush(); super.visitJumpInsn(op, l); }
        @Override public void visitLabel(Label l) { flush(); super.visitLabel(l); }
        @Override public void visitFieldInsn(int op, String o, String n, String d) { flush(); super.visitFieldInsn(op, o, n, d); }
        @Override public void visitLdcInsn(Object v) { flush(); super.visitLdcInsn(v); }
        @Override public void visitTypeInsn(int op, String t) { flush(); super.visitTypeInsn(op, t); }
        @Override public void visitIntInsn(int op, int v) { flush(); super.visitIntInsn(op, v); }
        @Override public void visitIincInsn(int v, int i) { flush(); super.visitIincInsn(v, i); }
        @Override public void visitInvokeDynamicInsn(String n, String d, Handle h, Object... a) { flush(); super.visitInvokeDynamicInsn(n, d, h, a); }
        @Override public void visitMaxs(int s, int l) { flush(); super.visitMaxs(s, l); }
    }

    /** Avant « new PlayerEntry(uuid, nom, numéro, enLigne) » : enLigne = CallHook.online(enLigne, uuid) (local 5 = uuid). */
    static final class AddEntry extends MethodVisitor {
        AddEntry(MethodVisitor mv) { super(Opcodes.ASM9, mv); }

        @Override public void visitMethodInsn(int op, String owner, String name, String desc, boolean itf) {
            if (op == Opcodes.INVOKESPECIAL && owner.equals("com/mikasrevs/newphone/network/PhoneStateData$PlayerEntry") && name.equals("<init>")
                    && desc.equals("(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Z)V")) {
                patched++;
                super.visitVarInsn(Opcodes.ALOAD, 5);
                super.visitMethodInsn(Opcodes.INVOKESTATIC, HOOK, "online", "(ZLjava/util/UUID;)Z", false);
            }
            super.visitMethodInsn(op, owner, name, desc, itf);
        }
    }
}
