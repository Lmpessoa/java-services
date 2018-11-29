/*
 * Copyright (c) 2018 Leonardo Pessoa
 * https://github.com/lmpessoa/java-services
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
package com.lmpessoa.services.views.templating;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.stream.Collectors;

/**
 * Specifies the requirements for renderisation engines.
 *
 * <p>
 * Classes implementing a renderization engine must implements this interface in order to be used as
 * renderization engines for the Views module.
 * </p>
 *
 * @param <T> the actual type of the template produced by the engine.
 */
public interface RenderizationEngine<T extends Template> {

   /**
    * Parses the given template.
    *
    * <p>
    * Engine subclasses implementing this interface must implement this method such that it returns
    * a parsed version of the given template. The parsed version can be used to render the template
    * with a given context.
    * </p>
    *
    * @param filename the name of the file to which the template belongs to.
    * @param template the contents of the template to be parsed.
    * @return the parsed template.
    * @throws TemplateParseException if there is an error with the syntax of the template.
    */
   T parse(String filename, String template) throws TemplateParseException;

   /**
    * Parses the template in the given file.
    *
    * <p>
    * The contents of the file are treated as if encoded using UTF-8. A different encoding can be
    * provided by calling {@link #parse(File, Charset)} instead.
    * </p>
    *
    * @param file the file whose contents are to be parsed.
    * @return the parsed template.
    * @throws IOException if an error occurs while trying to read the given file.
    * @throws TemplateParseException if there is an error with the syntax of the template.
    */
   default T parse(File file) throws IOException, TemplateParseException {
      return parse(file, StandardCharsets.UTF_8);
   }

   /**
    * Parses the template in the given file using the given encoding.
    *
    * @param file the file whose contents are to be parsed.
    * @param encoding the encoding used with the file.
    * @return the parsed template.
    * @throws IOException if an error occurs while trying to read the given file.
    * @throws TemplateParseException if there is an enrror with the syntax of the template.
    */
   default T parse(File file, Charset encoding) throws IOException, TemplateParseException {
      return parse(file.getName(), Files.readAllLines(file.toPath(), encoding).stream().collect(
               Collectors.joining("\n")));
   }
}
