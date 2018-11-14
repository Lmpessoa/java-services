/*
 * Copyright (c) 2017 Leonardo Pessoa
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
package com.lmpessoa.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.function.Function;

/**
 * Provides a simple interface for parsing command line arguments.
 *
 * <p>
 * The command line arguments accepted by this class are:
 * </p>
 *
 * <ul>
 * <li><em>options</em> which accept exactly one argument for configuration; or</li>
 * <li><em>flags</em> which only indicate the option is active or enabled.</li>
 * </ul>
 *
 * <p>
 * Options must define a function that will determine if the actual received value for the argument
 * is valid. This function may throw a runtime exception to indicate a failure. Despite being set
 * with different methods, options and flags share the same name database, thus there cannot exists
 * duplicate labels as options and flags at the same time. No exception is thrown is a label is
 * redefined.
 * </p>
 *
 * <p>
 * Once options and flags are defined, the actual list of arguments can be passed as argument to the
 * {@link #parse(String[])} method which will return a map with all values found on the argument
 * list (with no defaults) or throw an exception if there was an error while parsing the argument
 * list.
 * </p>
 */
public final class ArgumentReader {

   private Map<String, Function<String, Object>> options = new TreeMap<>();
   private Map<Character, String> mnemonics = new HashMap<>();

   /**
    * Defines an argument option with the given label.
    *
    * <p>
    * Options defined with this method are given explicitly the mnemonic associated with this option.
    * This value can also differ completely from the label or even be <code>null</code> to indicate
    * there should be no mnemonic for this option.
    * </p>
    *
    * @param label the name of the argument option.
    * @param mnemonic a mnemonic to use instead of the full name of the option.
    * @param type the function to validate and convert the argument value.
    */
   public void setOption(Character mnemonic, String label, Function<String, Object> type) {
      Objects.requireNonNull(type);
      if (!options.containsKey(label)) {
         options.put(label, type);
         if (mnemonic != null && !mnemonics.containsKey(mnemonic)) {
            mnemonics.put(mnemonic, label);
         }
      }
   }

   /**
    * Defines an argument flag with the given label.
    *
    * <p>
    * Flags defined with this method are given explicitly the mnemonic associated with this flag. This
    * value can also differ completely from the label or even be <code>null</code> to indicate there
    * should be no mnemonic for this flag.
    * </p>
    *
    * @param label the name of the argument flag.
    * @param mnemonic a mnemonic to use instead of the full name of the flag.
    */
   public void setFlag(Character mnemonic, String label) {
      if (!options.containsKey(label)) {
         options.put(label, null);
         if (mnemonic != null && !mnemonics.containsKey(mnemonic)) {
            mnemonics.put(mnemonic, label);
         }
      }
   }

   /**
    * Parses the given list of arguments with the defined set of options and flags.
    *
    * <p>
    * This map will always return the value <code>true</code> for flags. The value of options is
    * converted according to the function used to validate the value of that option and can be
    * converted by using a simple cast.
    * </p>
    *
    * @param args the list of arguments of the application to parse.
    * @return a map with the options and flags found in the arguments.
    */
   public Map<String, Object> parse(String[] arguments) {
      Map<String, Object> result = new HashMap<>();
      List<String> args = new ArrayList<>(Arrays.asList(arguments));
      while (!args.isEmpty()) {
         final String arg = args.remove(0);
         String modifier = null;
         if (arg.startsWith("--")) {
            modifier = arg.substring(2);
         } else if (arg.startsWith("-")) {
            char ch = arg.charAt(1);
            modifier = mnemonics.get(ch);
         }
         if (modifier == null || !options.containsKey(modifier)) {
            throw new IllegalArgumentException("Unknown option: " + arg);
         }
         Function<String, Object> type = options.get(modifier);
         if (type != null) {
            if (args.isEmpty() || args.get(0).startsWith("-")) {
               throw new IllegalStateException("Option requires an argument: " + arg);
            }
            String svalue = args.remove(0);
            try {
               Object value = type.apply(svalue);
               result.put(modifier, value);
            } catch (Exception e) {
               throw new IllegalArgumentException("Invalid value for " + arg + ": '" + svalue + "'");
            }
         } else {
            result.put(modifier, Boolean.TRUE);
         }
      }
      return result;
   }
}
