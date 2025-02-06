# 🚀 JPlugin Framework

Welcome to the **JPlugin Framework**! This framework is designed to simplify and streamline the development of plugins, enabling developers to create, manage, and utilize classes efficiently and intuitively.

## 📖 Table of Contents

1. [Overview](#overview)
2. [Installation](#installation)
3. [Project Structure](#project-structure)
4. [Annotations](#annotations)
   - [@Plugin](#plugin)
   - [@Initializer](#initializer)
   - [@Dependency](#dependency)
   - [@Category](#category)
5. [Usage Examples](#usage-examples)
   - [How to initialize the plugins](#plug-in-initialization-methods)
   - [Basic Plug-in Example](#basic-plug-in-example)
   - [Plug-in with Dependencies](#plug-in-with-dependencies)
   - [Using Categories](#using-categories)
6. [Advanced Plug-in Features](#advanced-plug-in-features)
   - [Custom Initializer Example](#custom-initializer-example)
7. [Considerations](#considerations)
8. [Troubleshooting](#troubleshooting)
   - [Common Issues](#common-issues)
   - [Contributing](#contributing)
9. [License](#license)

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

To install the **JPlugin Framework**, follow these steps:

1. **Clone the repository**:
   ```bash
   git clone https://github.com/ItsLaivy/jplugin.git
   cd jplugin
2. Add the dependency to your project. If you're using Maven, add the following to your pom.xml:

   ```xml
   <dependency>
       <groupId>codes.laivy</groupId>
       <artifactId>jplugin</artifactId>
       <version>1.0</version>
   </dependency>
3. Ensure that your IDE supports the framework and has all required configurations.
4. Check for any additional setup instructions provided in the documentation for specific environments (e.g., Maven, Gradle).

---

## Project Structure
The basic structure of the project is as follows:

```text
jplugin/
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── codes/
│   │   │       └── laivy/
│   │   │           └── plugin/
│   │   │               ├── annotation/
│   │   │               ├── category/
│   │   │               ├── exception/
│   │   │               ├── factory/
│   │   │                   └── handlers/
│   │   │               ├── info/
│   │   │               ├── initializer/
│   │   │               └── main/
│   │   └── resources/
│   └── test/
│       └── java/
│           └── codes/
│               └── laivy/
│                   └── plugin/
└── pom.xml
```

---

## Annotations
The JPlugin Framework uses several key annotations to define the behavior of plugins. Below is a detailed explanation of each annotation, along with their parameters and examples.

### @Plugin
The @Plugin annotation marks a class as a plugin component managed by the framework. It is the primary annotation that provides essential metadata about the plugin.

Usage
```java
@Plugin(name = "MyPlugin", description = "A sample plugin for demonstration purposes", version = "1.0.0")
public class MyPlugin {
   public static void initialize() {
      System.out.println("MyPlugin has been enabled!");
   }
   // Optional method
   public static void interrupt() {
      System.out.println("MyPlugin has been disabled!");
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
import codes.laivy.plugin.initializer.ConstructorPluginInitializer;

import java.io.Closeable;
import java.io.IOException;

@Initializer(type = ConstructorPluginInitializer.class)
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
@Dependency(type = SomeLibrary.class) // It can have multiples dependencies!
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
@Category(name = "Utility") // It can have multiples categories!
@Plugin(name = "Utility Plug-in", description = "A plugin that falls under the utility category.")
public class UtilityPlugin {
   public static void initialize() {
      System.out.println("Utility Plug-in is enabled!");
   }
}
```

```java
import codes.laivy.plugin.category.PluginHandler;
import codes.laivy.plugin.PluginInfo;
import codes.laivy.plugin.main.Plugins;
import org.jetbrains.annotations.NotNull;

public static void main(String[] args) {
   Plugins.getHandlers("Utility").add(new PluginHandler() {
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
1. `name`: A unique identifier for the category the plugin belongs to. This helps in filtering and managing plugins by functionality.

---

## Usage Examples
Now, let's explore some practical examples of using the JPlugin Framework. Each example demonstrates a different aspect of the framework.

### Plug-in initialization methods
When you create a class with the `@Plugin` annotation, if you simply execute your application it will not initialize at all. You need to initialize it manually, there's a lot of ways to do this.

#### Initialization using package
This is the most recommended way to load plugins, because it's faster.

```java
import codes.laivy.plugin.main.Plugins;

public static void main(String[] args) {
   // Plugins.initialize("com.project.plugins", false); // Will load all the plugins inside the package 'com.project.plugins'
   Plugins.initialize("com.project.plugins", true); // Will load all the plugins inside the package 'com.project.plugins' and the others packages inside it recursively
}
```

#### Initialization using plug-in finder
This is not the faster way to load plugins, but is the most precise one. You can control a lot of parameters to load plugins here.

```java
import codes.laivy.plugin.factory.PluginFinder;
import codes.laivy.plugin.main.Plugins;

public static void main(String[] args) {
   PluginFinder finder = Plugins.find();
   finder.addPackage("codes.laivy"); // Will load all plug-ins into this package (recursively by default)
   finder.addClassLoader(Main.class.getClassLoader()); // Using a specific class loader
   finder.categories("HTTP Page"); // Only plug-ins with a specific category
   
   // Load all and print to the console
   System.out.println("Successfully loaded " + finder.load().length + " plug-ins.");
}
```

### Basic Plug-in Example
Here’s how to create a simple plug-in using the framework. This example will demonstrate the minimal setup required to get a plug-in running.

```java
@Plugin(name = "HelloWorldPlugin", description = "A simple plug-in that greets users.", version = "1.0.0")
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

import codes.laivy.plugin.annotation.Initializer;
import codes.laivy.plugin.initializer.ConstructorPluginInitializer;

/**
 * This is a plug-in that represents an HTTP page at an website environment
 * As you can see, this class is private and final, but the JPlugin still can access it.
 * <p>
 * It uses the {@link ConstructorPluginInitializer} that initializes using the class constructor, not the {@code initialize} methods.
 */
@Category(name = "HTTP Page")
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
import codes.laivy.plugin.category.PluginHandler;
import codes.laivy.plugin.exception.PluginInitializeException;
import codes.laivy.plugin.PluginInfo;
import codes.laivy.plugin.main.Plugins;
import org.jetbrains.annotations.NotNull;

public static void main(String[] args) {
   // Create 'HTTP Page' category and add a plug-in handler to it
   Plugins.getHandlers("HTTP Page").add(new HTTPPageHandler());

   // Initialize all plug-ins at the "my.website.pages" package, recursively.
   Plugins.find().addPackage("my.website.pages", true).load();

   // Plugins.find().addPackage("my.website.pages", true).load((c) -> c.getSuperclass() == Page.class); // Load only plug-ins that extends Page, to avoid issues.
}

// Classes

private static final class HTTPPageHandler implements PluginHandler {
   @Override
   public boolean accept(@NotNull PluginInfo info) {
      // Check if the class extends Page
      // If a class with the 'HTTP Page' category but that doesn't extends the Page class
      // Tries to load, this method will be called. And if returned false, the loading will be interrupted.
      boolean allow = info.getReference().getSuperclass() == Page.class;

      if (!allow) {
         System.out.println("The class " + info.getReference().getName() + " is trying to load as HTTP Page but doesn't extends Page!");
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

**Explanation**: This plug-in easily creates an authentication page using categories, you just need to create a class that extends Page, add the `@Category(name = "HTTP Page")` to it, mark as a `@Plugin` and it's done.

---

## Advanced Plug-in Features
The plug-in has advanced features that allows developers enhance the power of the plug-ins and controls a lot of things more!

### Custom Initializer Example
You can define custom initialization logic for your plug-in using an initializer, allowing for more complex setup procedures.

```java
public final class MyPluginInitializer extends PluginInitializer {

   // Every plug-in initializer *MUST* have an empty declared constructor like that.
   private MyPluginInitializer() {
   }

   @Override
   public @NotNull PluginInfo create(@NotNull Class<?> reference, @Nullable String name, @Nullable String description, @NotNull PluginInfo @NotNull [] dependencies, @NotNull String @NotNull [] categories) {
      // Creates a PluginInfo with your own initialization mechanic here (custom #start and #stop methods)
   }
   
}
   
```

```java
@Initializer(type = MyPluginInitializer.class)
@Plugin(name = "CustomInitializerPlugin", description = "A plug-in with a custom initializer.")
public class CustomInitializerPlugin {
   // The initialization method should be created by you.
}
```
**Explanation**: The MyPluginInitializer class creates a custom PluginInfo object with custom `#start` and `#stop` methods, allowing to change completely how the plug-in should be initialized/interrupted.

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