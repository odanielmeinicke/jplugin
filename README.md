# 🚀 JPlugin Framework

Welcome to the **JPlugin Framework**! This framework is designed to simplify and streamline the development of plugins, enabling developers to create, manage, and utilize classes efficiently and intuitively.

> Don't forget to see the [JPlugin Wiki](https://github.com/odanielmeinicke/jplugin/wiki), there's a lot of features that i didn't included here!

## 📖 Table of Contents

1. [Overview](#overview)
2. [Installation](#installation)
3. [Project Structure](#project-structure)
4. [Annotations](#annotations)
   - [@Plugin](#plugin)
   - [@Initializer](#initializer)
   - [@Dependency](#dependency)
   - [@Category](#category)
   - [@Priority](#priority)
   - and some others, check the wiki...
5. [Usage Examples](#usage-examples)
   - [How to initialize the plugins](#plug-in-initialization-methods)
   - [Basic Plug-in Example](#basic-plug-in-example)
   - [Plug-in with Dependencies](#plug-in-with-dependencies)
   - [Using Categories](#using-categories)
6. [Considerations](#considerations)
7. [Troubleshooting](#troubleshooting)
   - [Common Issues](#common-issues)
   - [Contributing](#contributing)
8. [License](#license)

---

## Overview

The **JPlugin Framework** provides a set of tools to facilitate modular plugin development. With annotations for automatic plugin management, this framework simplifies the plugin lifecycle, from initialization to execution.

### 🌟 Features

- **Plug-in Management**: Annotations that allow for the definition and discovery of plugins.
- **Dependency Injection**: Declare necessary dependencies for your plugin's operation.
- **Categorization**: Organize plugins into categories for better management.
- **Dynamic Initialization**: Specify how a plugin should be initialized through custom implementations.
- **Event Handling**: Built-in support for handling events across plugins.
- **Configuration Management**: Easy management of configuration files for plugins.

---

## Installation

For now, there's no public artifact at the Maven Central for this.
To use the **JPlugin** framework.
You should install it manually at your project
using [Maven Guide to installing 3rd party JARs](https://maven.apache.org/guides/mini/guide-3rd-party-jars-local.html)

---

## Project Structure
The basic structure of the project is as follows:

```text
jplugin/
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── dev/
│   │   │       └── meinicke/
│   │   │           └── plugin/
│   │   │               ├── annotation/
│   │   │               ├── attribute/
│   │   │               ├── category/
│   │   │               ├── context/
│   │   │               ├── exception/
│   │   │               ├── factory/
│   │   │                   └── handlers/
│   │   │               ├── initializer/
│   │   │               ├── main/
│   │   │               └── metadata/
│   │   └── resources/
│   └── test/
│       └── java/
│           └── dev/
│               └── meinicke/
│                   └── plugin/
│                       └── I know, tests are very important, but i'm still creating!
└── pom.xml
```

---

## Annotations
The JPlugin Framework uses several key annotations to define the behavior of plugins. Below is a detailed explanation of each annotation, along with their parameters and examples.

### @Plugin
The @Plugin annotation marks a class as a plugin component managed by the framework. It is the primary annotation that provides essential metadata about the plugin.

Usage

```java
import java.io.Closeable;
import java.io.IOException;

@Plugin(name = "MyPlugin", description = "A sample plugin for demonstration purposes")
public final class MyPlugin implements Closeable {

   public MyPlugin() {
      System.out.println("MyPlugin has been enabled!");
   }
   
   @Override
   public void close() {
      System.out.println("MyPlugin has been disabled!");
      // The Closeable interface is optional!
   }
   
}
```

#### Parameters
1. `name`: A human-readable name for the plugin. Defaults to the class name if empty.
2. `description`: A brief description of the plugin's functionality.

### @Initializer
The @Initializer annotation specifies the implementation responsible for initializing a plugin. This allows for custom setup logic during the plugin lifecycle.

Usage

```java
import dev.meinicke.plugin.initializer.ConstructorPluginInitializer;

import java.io.Closeable;

@Initializer(type = ConstructorPluginInitializer.class) // This is the default initializer!
@Plugin(name = "Initializer Plug-in", description = "A plugin with a custom initializer.")
public class InitializerPlugin implements Closeable {
   public InitializerPlugin() {
      System.out.println("Plug-in has been enabled!");
   }

   @Override
   public void close() {
      System.out.println("Plug-in has been disabled!");
   }

}
```

#### Parameters
1. `type`: Specifies the PluginInitializer implementation used to initialize the plugin.

### @Dependency
The `@Dependency` annotation declares a required dependency for a plugin class. This is essential for ensuring that all necessary components are available before a plugin is loaded.

Usage

```java
import dev.meinicke.plugin.annotation.Initializer;
import dev.meinicke.plugin.initializer.MethodPluginInitializer;

@Dependency(type = SomeLibrary.class) // It can have multiples dependencies!
@Initializer(type = MethodPluginInitializer.class)
@Plugin(name = "LibraryDependentPlugin", description = "A plugin that uses SomeLibrary.")
public class LibraryDependentPlugin {
   public static void initialize() {
      // This plugin only will be initialized when SomeLibrary has initialized.
      SomeLibrary.doSomething();
   }

   public static void interrupt() {
      // This plugin will be interrupted BEFORE the dependencies
   }
}
```

#### Parameters
- `type`: Specifies the class that represents the required dependency. The framework ensures that the dependency is loaded before the plugin.

### @Category
The @Category annotation represents a category for a plugin, allowing the addition of special handlers for plugins in that category. This aids in organizing and managing plugins based on functionality.

Usage

```java
@Category("Utility") // It can have multiples categories!
@Initializer(type = MethodPluginInitializer.class)
@Plugin(name = "Utility Plug-in", description = "A plugin that falls under the utility category.")
public class UtilityPlugin {
   public static void initialize() {
      System.out.println("Utility Plug-in is enabled!");
   }
}
```

```java
import dev.meinicke.plugin.factory.handlers.PluginHandler;
import dev.meinicke.plugin.PluginInfo;
import dev.meinicke.plugin.main.Plugins;
import org.jetbrains.annotations.NotNull;

public static void main(String[] args) {
   // It will create a new category called 'Utility' and add a custom handler
   Plugins.getCategory("Utility").getHandlers().add(new PluginHandler() {
      @Override
      public void start(@NotNull PluginInfo info) {
         System.out.println("The utility plugin '" + info + "' has been started.");
         // Do cool stuff here for all Utility category plugins
      }
   });

   // Initializes all the plugins (incluidng the UtilityPlugin)
   Plugins.initializeAll();
}
```

#### Parameters
1. `value`: A unique identifier for the category the plugin belongs to. This helps in filtering and managing plugins by functionality.

### @Priority

The **@Priority** annotation defines the loading priority of a plugin within the system. It allows developers to specify an integer value that determines the order in which plugins are initialized. In this context, lower integer values represent higher priority, meaning that plugins with lower numbers are loaded before those with higher numbers.

#### Default Behavior
- **No Annotation:** If a plugin does not have the **@Priority** annotation, it is assumed to have a default priority of `0`.
- **Annotation Without a Value:** If a plugin is annotated with **@Priority** without explicitly specifying a value, it defaults to `-1`. This effectively gives such plugins a higher priority compared to plugins that rely on the default `0`.

#### Loading Order
Plugins are loaded in ascending order based on their priority values. For example:
- A plugin annotated with `@Priority(-5)` will be loaded **before** a plugin annotated with `@Priority(2)`.

#### Dependency Considerations
In frameworks that are dependency-aware, the dependency relationships always take precedence over numeric priority values. This means that if Plugin A is a dependency of Plugin B, Plugin A will be loaded first, regardless of the numerical priorities assigned.

#### Usage Examples
```java
// A plugin with a higher priority (loaded earlier)
@Priority(1)
@Plugin(name = "CorePlugin", description = "Handles core system functions")
public class CorePlugin {
   // Plugin implementation details...
}

// A plugin with a lower priority (loaded later)
@Priority(10)
@Plugin(name = "ExtraPlugin", description = "Provides additional features")
public class ExtraPlugin {
   // Plugin implementation details...
}

// A plugin with default priority (-1), which will load before plugins without @Priority annotation
@Priority
@Plugin(name = "MiddlewarePlugin", description = "Handles middleware tasks")
public class MiddlewarePlugin {
   // Plugin implementation details...
}
```

---

## Usage Examples
Now, let's explore some practical examples of using the JPlugin Framework. Each example demonstrates a different aspect of the framework.

### Plug-in initialization methods
When you create a class with the `@Plugin` annotation, if you simply execute your application it will not initialize at all. You need to initialize it manually, there's a lot of ways to do this.

#### Initialization using package
This is the most recommended way to load plugins, because it's faster.

```java
import dev.meinicke.plugin.main.Plugins;

public static void main(String[] args) {
   // Plugins.initialize("com.project.plugins", false); // Will load all the plugins inside the package 'com.project.plugins'
   Plugins.initialize("com.project.plugins", true); // Will load all the plugins inside the package 'com.project.plugins' and the others packages inside it recursively
}
```

#### Initialization using plug-in finder
This is not the faster way to load plugins, but is the most precise one. You can control a lot of parameters to load plugins here.

```java
import dev.meinicke.plugin.factory.PluginFinder;
import dev.meinicke.plugin.main.Plugins;

public static void main(String[] args) {
   PluginFinder finder = Plugins.find();
   finder.addPackage("dev.meinicke"); // Will load all plug-ins into this package (recursively by default)
   finder.addClassLoader(Main.class.getClassLoader()); // Using a specific class loader
   finder.categories("HTTP Page"); // Only plug-ins with a specific category
   
   // Load all and print to the console
   System.out.println("Successfully loaded " + finder.load().length + " plug-ins.");
}
```

### Basic Plug-in Example
Here’s how to create a simple plug-in using the framework. This example will demonstrate the minimal setup required to get a plug-in running.

```java
@Initializer(type = MethodPluginInitializer.class)
@Plugin(name = "HelloWorldPlugin", description = "A simple plug-in that greets users.")
public class HelloWorldPlugin {
   public static void initialize() {
      System.out.println("Hello, World!");
   }
   public static void interrupt() {
      System.out.println("Goodbye, World!");
   }
}
```

**Explanation**: The onEnable method is called when the plug-in is loaded, and it will print "Hello, World!" to the console. The onDisable method is called when the plug-in is unloaded.

### Plug-in with Dependencies
Let's create a plug-in that depends on an external library, ensuring that the required dependencies are available at runtime.

```java
/**
 * First, it loads all the books (Book plug-in) before the bookshelf to control them.
 */
@Dependency(type = Book.class)
@Initializer(type = MethodPluginInitializer.class)
@Plugin(name = "Bookshelf", description = "A mastered controller of the books")
public class Bookshelf {
   public static void initialize() {
      // It is safe to execute the Book's plug-in methods because the Book plug-in
      // is active and running with all the features.
      System.out.println("Initializing bookshelf...");
      System.out.println("Detected " + Book.count() + " available books.");
   }
}
```
**Explanation**: This plug-in will only be enabled if SomeLibrary is available in the project. The onEnable method calls a method from the external library to demonstrate interaction.

### Using Categories
Organizing plug-ins into categories can help in managing them effectively. Here’s how to categorize a plug-in for better organization.

```java
package my.website.pages;

import dev.meinicke.plugin.annotation.Initializer;
import dev.meinicke.plugin.initializer.ConstructorPluginInitializer;

/**
 * This is a plug-in that represents an HTTP page at an website environment
 * As you can see, this class is private and final, but the JPlugin still can access it.
 * <p>
 * It uses the {@link ConstructorPluginInitializer} that initializes using the class constructor, not the {@code initialize} methods.
 */
@Category("HTTP Page")
@Initializer(type = ConstructorPluginInitializer.class) // Initializes using the empty declared constructor
@Plugin(name = "Authentication Page", description = "The authentication page that allow users to access the dashboard at the website.")
final class Authentication extends Page {

   // The constructor must have no parameters, and could have any visilibity.
   private Authentication() {
      super("/authentication"); // The page path
   }

   @Override
   public void execute(HttpClient client, HttpData data) {
      // Do stuff here
   }

}
```

```java
import dev.meinicke.plugin.factory.handlers.PluginHandler;
import dev.meinicke.plugin.exception.PluginInitializeException;
import dev.meinicke.plugin.PluginInfo;
import dev.meinicke.plugin.main.Plugins;
import org.jetbrains.annotations.NotNull;

public static void main(String[] args) {
   // Create 'HTTP Page' category and add a plug-in handler to it
   Plugins.getCategory("HTTP Page").getHandlers().add(new HTTPPageHandler());

   // Initialize all plug-ins at the "my.website.pages" package, recursively.
   Plugins.find().addPackage("my.website.pages", true).load();

   // Plugins.find().addPackage("my.website.pages", true).load((c) -> c.getSuperclass() == Page.class); // Load only plug-ins that extends Page, to avoid issues.
}

// Classes

private static final class HTTPPageHandler implements PluginHandler {
   @Override
   public boolean accept(@NotNull PluginInfo.Builder builder) {
       return checkReference(builder.getReference());
   }
   @Override // Take a read at the javadocs to know the difference between those two methods!
   public boolean accept(@NotNull PluginInfo info) {
       return checkReference(info.getReference());
   }
   
   public boolean checkReference(@NotNull Class<?> reference) {
      // Check if the class extends Page
      // If a class with the 'HTTP Page' category but that doesn't extends the Page class
      // Tries to load, this method will be called. And if returned false, the loading will be interrupted.
      boolean allow = info.getReference().getSuperclass() == Page.class;
      if (!allow) {
         System.out.println("The class " + builder.getReference().getName() + " is trying to load as HTTP Page but doesn't extends Page!");
      }
      
      return allow;
   }

   @Override
   public void start(@NotNull PluginInfo info) throws PluginInitializeException {
      // When the plug-in starts, it adds the Page instance to the HTTP Environment to start
      // receiving connections as a valid http page
      HTTPEnvironment.getPages().add((Page) info.getInstance());
   }
}

```

**Explanation**: This plug-in easily creates an authentication page using categories, you just need to create a class that extends Page, add the `@Category("HTTP Page")` to it, mark as a `@Plugin` and it's done.

---

## Considerations
- Thread Safety: Ensure that any shared resources among plug-ins are properly synchronized to avoid concurrency issues.
- Dependency Management: Clearly define and document the dependencies required for each plug-in to function properly.
- Version Compatibility: Maintain backward compatibility when updating the framework or existing plug-ins.

---

## Troubleshooting
### Common Issues
1. **Plug-in Not Loading**: If you created the plug-in correctly but the initialization hasn't been performing, check if you're starting the framework using `Plugins.initializeAll()` method or similar.

### Contributing
We welcome contributions to the JPlugin Framework! To contribute:

1. Fork the repository.
2. Create a new branch for your feature or bugfix.
3. Make your changes and write tests to cover new functionality.
4. Submit a pull request detailing your changes.

---

## License
This project is licensed under the MIT License. Feel free to use, modify, and distribute it as you wish, with proper attribution to the original authors.