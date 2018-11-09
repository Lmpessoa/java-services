/*
 * Copyright (c) 2017 Leonardo Pessoa
 * http://github.com/lmpessoa/java-services
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.lmpessoa.services.routing.content;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.StringReader;
import java.io.StringWriter;
import java.nio.charset.Charset;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;

final class XmlSerializer implements IContentParser, IContentProducer {

   @Override
   @SuppressWarnings("unchecked")
   public <T> T parse(String content, Class<T> clazz) {
      try {
         JAXBContext context = JAXBContext.newInstance(clazz);
         Unmarshaller unmarshaller = context.createUnmarshaller();
         return (T) unmarshaller.unmarshal(new StringReader(content));
      } catch (JAXBException e) {
         throw new RuntimeException(e);
      }
   }

   @Override
   public InputStream produce(Object obj) {
      try {
         JAXBContext context = JAXBContext.newInstance(obj.getClass());
         Marshaller marshaller = context.createMarshaller();
         StringWriter result = new StringWriter();
         marshaller.marshal(obj, result);
         byte[] data = result.toString().getBytes(Charset.forName("UTF-8"));
         return new ByteArrayInputStream(data);
      } catch (JAXBException e) {
         throw new RuntimeException(e);
      }
   }
}