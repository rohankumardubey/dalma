package test;

import dalma.ports.email.EmailEndPoint;
import dalma.ports.email.NewMailHandler;
import dalma.ports.email.POP3Listener;
import dalma.test.Launcher;
import dalma.test.PasswordStore;

import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import java.io.Serializable;
import java.util.UUID;

/**
 * @author Kohsuke Kawaguchi
 */
public class EmailTest extends Launcher implements NewMailHandler {
    public EmailTest(String[] args) throws Exception {
        super(args);
    }

    public static void main(String[] args) throws Exception {
        new EmailTest(args);
    }

    EmailEndPoint ep;

    protected void setUpEndPoints() throws Exception {
        ep = new EmailEndPoint(
            "email",
            new InternetAddress("dalma@kohsuke.org","dalma engine"),
            new POP3Listener("mail.kohsuke.org","dalma",PasswordStore.get("dalma@kohsuke.org"),3000));
        ep.setNewMailHandler(this);
        engine.addEndPoint(ep);
    }

    public void onNewMail(MimeMessage mail) throws Exception {
        System.out.println("new e-mail");
        createConversation(ConversationImpl.class,ep,mail);
    }

    public static final class ConversationImpl implements Runnable, Serializable {
        private final EmailEndPoint ep;

        // initial e-mail
        private MimeMessage email;

        public ConversationImpl(EmailEndPoint ep, MimeMessage email) {
            this.ep = ep;
            this.email = email;
        }

        public void run() {
            try {
                UUID uuid = UUID.randomUUID();

                System.out.println("started "+uuid);
                MimeMessage msg = email;
                int count = 0;

                while(true) {
                    if(msg.getContent().toString().contains("bye"))
                        break;

                    // reply
                    msg = (MimeMessage)msg.reply(false);
                    msg.setText("Hello! "+(count++));
                    msg.setSubject("testing dalma "+uuid.toString());
                    msg = ep.waitForReply(msg);
                    System.out.println("got a reply.");
                }

                MimeMessage reply = (MimeMessage)msg.reply(false);
                reply.setText("bye bye");
                ep.send(reply);
                System.out.println("done");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        private static final long serialVersionUID = 1L;
    }
}
