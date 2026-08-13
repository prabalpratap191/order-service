---
name: Fix PostgreSQL TimeZone Configuration Error
overview: Resolve the invalid TimeZone parameter error ('Asia/Calcutta') preventing Hibernate SessionFactory initialization in Spring Boot application
userQuery: |-
  Caused by: org.springframework.beans.factory.BeanCreationException: Error creating bean with name 'entityManagerFactory' defined in class path resource [org/springframework/boot/autoconfigure/orm/jpa/HibernateJpaConfiguration.class]: [PersistenceUnit: default] Unable to build Hibernate SessionFactory; nested exception is org.hibernate.exception.DataException: Unable to open JDBC Connection for DDL execution [FATAL: invalid value for parameter "TimeZone": "Asia/Calcutta"] [n/a]
          at org.springframework.beans.factory.support.AbstractAutowireCapableBeanFactory.initializeBean(AbstractAutowireCapableBeanFactory.java:1786) ~[spring-beans-6.1.6.jar:6.1.6]
          at org.springframework.beans.factory.support.AbstractAutowireCapableBeanFactory.doCreateBean(AbstractAutowireCapableBeanFactory.java:600) ~[spring-beans-6.1.6.jar:6.1.6]
          at org.springframework.beans.factory.support.AbstractAutowireCapableBeanFactory.createBean(AbstractAutowireCapableBeanFactory.java:522) ~[spring-beans-6.1.6.jar:6.1.6]
          at org.springframework.beans.factory.support.AbstractBeanFactory.lambda$doGetBean$0(AbstractBeanFactory.java:326) ~[spring-beans-6.1.6.jar:6.1.6]
          at org.springframework.beans.factory.support.DefaultSingletonBeanRegistry.getSingleton(DefaultSingletonBeanRegistry.java:234) ~[spring-beans-6.1.6.jar:6.1.6]
          at org.springframework.beans.factory.support.AbstractBeanFactory.doGetBean(AbstractBeanFactory.java:324) ~[spring-beans-6.1.6.jar:6.1.6]
          at org.springframework.beans.factory.support.AbstractBeanFactory.getBean(AbstractBeanFactory.java:200) ~[spring-beans-6.1.6.jar:6.1.6]
          at org.springframework.context.support.AbstractApplicationContext.getBean(AbstractApplicationContext.java:1234) ~[spring-context-6.1.6.jar:6.1.6]
          at org.springframework.context.support.AbstractApplicationContext.finishBeanFactoryInitialization(AbstractApplicationContext.java:952) ~[spring-context-6.1.6.jar:6.1.6]
          at org.springframework.context.support.AbstractApplicationContext.refresh(AbstractApplicationContext.java:624) ~[spring-context-6.1.6.jar:6.1.6]
          at org.springframework.boot.SpringApplication.refresh(SpringApplication.java:754) ~[spring-boot-3.2.5.jar:3.2.5]
          at org.springframework.boot.SpringApplication.refreshContext(SpringApplication.java:456) ~[spring-boot-3.2.5.jar:3.2.5]

  Getting this error
createdAt: '2026-08-12T09:43:12.154Z'
agentPhase: execution
todos:
  - id: 1
    content: Search for 'Asia/Calcutta' references in configuration files
    status: pending
  - id: 2
    content: Update timezone configuration in application.properties
    status: pending
  - id: 3
    content: Update timezone configuration in application.yml
    status: pending
  - id: 4
    content: Verify and update JDBC connection URL timezone parameters
    status: pending
  - id: 5
    content: Document timezone configuration changes
    status: pending
executionStartedAt: '2026-08-12T09:43:56.024Z'
---

# Problem Analysis

The application fails to start because PostgreSQL rejects the timezone value `Asia/Calcutta`. Modern PostgreSQL versions require IANA timezone identifiers, and `Asia/Calcutta` was renamed to `Asia/Kolkata` in the IANA timezone database.

## Root Cause

Hibernate is attempting to establish a JDBC connection with timezone `Asia/Calcutta`, which PostgreSQL no longer recognizes as valid. This occurs during DDL execution when initializing the EntityManagerFactory.

# Solution Strategy

## Primary Fix: Update Timezone Configuration

Locate and update the timezone configuration in the application to use the correct IANA identifier `Asia/Kolkata` instead of the deprecated `Asia/Calcutta`.

### Configuration Locations to Check

1. **Application Properties/YAML**: Check [application.properties](src/main/resources/application.properties) or [application.yml](src/main/resources/application.yml) for:
   - `spring.jpa.properties.hibernate.jdbc.time_zone`
   - `spring.datasource.url` (connectionTimeZone parameter)
   - JVM timezone settings

2. **Database Connection URL**: Look for timezone parameters in JDBC URL:
   - PostgreSQL: `?serverTimezone=Asia/Calcutta`
   - MySQL: `?connectionTimeZone=Asia/Calcutta`

3. **JVM Arguments**: Check for `-Duser.timezone=Asia/Calcutta` in run configurations

4. **Docker/Environment Variables**: Verify `TZ` environment variable if running in containers

## Configuration Changes Required

Replace all occurrences of `Asia/Calcutta` with `Asia/Kolkata` in:
- Spring datasource configuration
- Hibernate timezone properties
- JVM system properties
- Environment variables

## Alternative Approaches

### Option 1: Use UTC (Recommended for Production)
Set timezone to UTC to avoid regional timezone complexities:
- `spring.jpa.properties.hibernate.jdbc.time_zone=UTC`
- Store all timestamps in UTC, convert to local timezone in application layer

### Option 2: Use Offset-Based Timezone
Use `+05:30` (IST offset) if IANA identifiers cause issues:
- `spring.jpa.properties.hibernate.jdbc.time_zone=+05:30`

# Validation Steps

1. Search configuration files for `Asia/Calcutta` references
2. Update to `Asia/Kolkata` or `UTC`
3. Restart application and verify successful startup
4. Check application logs for timezone-related warnings
5. Verify database timestamp handling matches expectations

# Design Considerations

**Timezone Strategy**: For production systems, consider:
- Store all timestamps in UTC in database
- Convert to user's local timezone in presentation layer
- Avoid hardcoding regional timezones in configuration
- Use application-level timezone conversion for business logic

**Configuration Management**: 
- Externalize timezone configuration to environment-specific property files
- Use Spring profiles for different deployment environments
- Document timezone handling strategy in project documentation