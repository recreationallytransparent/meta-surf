package com.pickard.metasurf.scrapers
import net.ruippeixotog.scalascraper.browser.JsoupBrowser
import net.ruippeixotog.scalascraper.model.Document

trait DocumentProvider {
  def get(url: String): Document
}

class JSoupDocumentProvider extends DocumentProvider {
  private lazy val client = new JsoupBrowser()
  override def get(url: String): Document = client.get(url)
}