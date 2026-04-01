package com.kafka.emailecho.mail.integration.service;

import com.sun.mail.imap.IMAPFolder;
import jakarta.mail.MessagingException;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class EmailListenerServiceImpl implements Runnable {

  private static final long KEEP_ALIVE_FREQ = 300000; // 5 minutes
  private IMAPFolder folder;

  public EmailListenerServiceImpl(IMAPFolder folder) {
    this.folder = folder;
  }

  @Override
  public void run() {
    while (!Thread.interrupted()) {
      try {
        Thread.sleep(KEEP_ALIVE_FREQ);
        log.info("Performing a NOOP to keep the connection alive");
        folder.doCommand(protocol -> {
          protocol.simpleCommand("NOOP", null);
          return null;
        });
      } catch (InterruptedException e) {
        e.printStackTrace();
      } catch (MessagingException e) {
        log.error("Unexpected exception while keeping alive the IDLE connection");
        e.printStackTrace();
      }
    }
  }

}
