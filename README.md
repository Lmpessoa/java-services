A lightweight and easy engine for developing web APIs and microservices.

Our goal is to enable developers to create REST microservices in Java as fast, fun, simple, and accessible as possible yet enabling the such applications to be reliable and production-ready with minimal knowledge of the framework itself.

## Background

This project was strongly inspired by ASP.NET Core, which presented itself with a simple design, easy upfront development and yet with a strong capability for extension, however we chose to focus on the development of microservices instead of full web applications (don't worry, you can also do it if needed).

We've given a lot of thought into how we'd like writing microservices to be done. Thus, we wanted to be able to:
* use of convention over configuration so you won't waste time over configuration details to start writing your services;
* use of basic Java so even if you just started learning the language you can still start creating microservices;
* need for no external tool (CLI) so you don't have to go back and forth between your prefered IDE and a terminal;
* run as its own application so you won't waste time setting up a separate server before you actually need one;
* have a minimal set of dependencies so your workspace won't be bloated with loading dependencies that are not useful to you;
* be a complete solution so you don’t have to mix and match dependencies to start writing your app.

## Usage

Although it is possible to use it without Maven, it was designed to be more easily used within a Maven project. To start with Maven, all you need to do is to include a single dependency to your POM file:

```xml
   ...
   <dependency>
      <groupId>com.lmpessoa.services</groupId>
      <artifactId>core</artifactId>
      <version>1.0</version>
   </dependency>
   ...
```

After your dependencies are updated, all you need to do is start the application. The best (and most recommended) place to do it is right on the main method of the application. The following is an example of a minimal class required to start the engine. Go ahead and run it, we'll wait.

```java
package com.example;

import com.lmpessoa.services.core.Application;

public class SampleMicroservice {

   public static void main(String[] args) {
      Application.startWith(args);
   }
}
```

Since it runs as a standalone aplication, you might want to pack it in a JAR file before you deploy. Thus, you'll need to indicate this is the main class for your project. You can do that [by hand](https://docs.oracle.com/javase/tutorial/deployment/jar/appman.html) or [using Maven](https://www.mkyong.com/maven/how-to-create-a-manifest-file-with-maven/).

Once running, the engine will publish methods from each class named with the suffix “Resource” that exists in packages ending with “.resources”. The remaining name of the class will be used to create the path where the resource will be accessible. The published methods match the name of the HTTP method used when calling the resource and any arguments the method expects are extracted from the requested URL.

```java
package com.example.resources;

// no need to inherit anything
public class BooksResource {

   // will respond to http://localhost:5617/books
   public List<Book> get() {
      ...
   }

   // will respond to http://localhost:5617/books/7
   public Book get(int id) {
      ...
   }

   // will respond to http://localhost:5617/books/123-456-7890
   // note if it were not for the dashes, the previous method would have been called instead
   public Book get(String isbn) {
      ...
   }
}
```

That’s just it! Now go write some services yourself!

## Disclaimer

This project originally used a different name/namespace for which I no longer maintain the domain. Since the project has never been published before, it was my decision to change the namespace from the very first commit in order to use only the current namespace. The multiple projects that form the entire framework have also been merged into one single repository, even thou it is not best practice, in order to facilitate this migration and possible future maintenance of the project.

Since this is an open source project under MIT, anyone is entitled to do whatever they please from this project including but not limited to use it on your own projects or derive into a new project, even replacing the base package name if you please, as long as the original authorship of the project is retained.

The original commit date this change is: Sun Oct 29 17:15:15 2017 -0200
