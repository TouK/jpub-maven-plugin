jpub-maven-plugin
=================

##Introduction

Maven3 plugin that integrates Oracle JPublisher into Maven project lifecycle. 
It generates Java classes with methods for every stored procedure you specify from your Oracle Database,
and you do not have to worry about mapping types, including complex types, lists etc..

##Requirments

JPublisher artifacts.
First you have to install JPublisher artifacts in your maven repository. 
So download jpublisher distribution from 
http://www.oracle.com/technology/software/tech/java/sqlj_jdbc/index.html
and install it your favorite way in your repository.
I have installed them as:

```
<dependency>
	<groupId>oracle</groupId>
	<artifactId>runtime12</artifactId>
	<version>10.2</version>
</dependency>
<dependency>
	<groupId>oracle</groupId>
	<artifactId>translator</artifactId>
	<version>10.2</version>
</dependency>
```

##Oracle Driver

JPublisher generated classes work only with unwrapped JDBC Driver. This is some kind of problem if you are using for example WebLogic datasource (but as Oracle has bought WebLogic recently, this problem should disappear in near future).
For now you have configure your own datasource using OracleDataSource included in oracle driver.
I made jdbc driver available in my repository as:

```
<dependency>
   <groupId>oracle</groupId>
	<artifactId>ojdbc5</artifactId>
	<version>11.1.0.6.0</version>
</dependency>
<dependency>
	<groupId>oracle</groupId>
	<artifactId>orai18n</artifactId>
	<version>11.1.0.6.0</version>
</dependency>
```

##Plugin usage

To use the plugin you just need to add following configuration to you pom.xml:

```
...
   <build>
      ...
       <plugins>
          ...
            <plugin>
                <groupId>pl.touk.top</groupId>
                <artifactId>jpub-maven-plugin</artifactId>
                <version>2.0.0</version>
                <configuration>
                    <outputDirectory>${project.build.outputDirectory}</outputDirectory>
                    <user>${database.user}/${database.password}</user>
                    <url>${database.url}</url>
                    <sourceDirectory>${project.build.directory}/generated-sources/src</sourceDirectory>
                    <genPackage>pl.touk.myexample.dao.oracle</genPackage>
                    <encoding>UTF-8</encoding>
                    <compile>true</compile>
                    <connscope>method</connscope>
                    <toString>true</toString>
                    <other>-omit_schema_names -datasource=true -connscope=method -compatible=9i 
-numbertypes=oracle -input=${project.basedir}/src/main/resources/jpub_object_list</other>
                    <debug>true</debug>
                    <closeConnection>true</closeConnection>
                    <xmx>256m</xmx>
                    <xms>256m</xms>
                    <skip>${skipJPub}</skip>
                </configuration>
                <executions>
                    <execution>
                        <phase>generate-sources</phase>
                        <goals>
                            <goal>unpack</goal>
                        </goals>
                    </execution>
                </executions>
                <dependencies>
                    <dependency>
                       <groupId>oracle</groupId>
                        <artifactId>ojdbc5</artifactId>
                        <version>11.1.0.6.0</version>
                    </dependency>
                    <dependency>
                        <groupId>oracle</groupId>
                        <artifactId>orai18n</artifactId>
                        <version>11.1.0.6.0</version>
                    </dependency>
                    <dependency>
                        <groupId>oracle</groupId>
                        <artifactId>runtime12</artifactId>
                        <version>10.2</version>
                    </dependency>
                    <dependency>
                        <groupId>oracle</groupId>
                        <artifactId>translator</artifactId>
                        <version>10.2</version>
                    </dependency>
                </dependencies>
            </plugin>
         ...
      </plugins>
     ...
    </build>
```

That will generate Java classes for stored procedures listed in -input=${project.basedir}/src/main/resources/jpub_object_list 
All parameters specified in <configuration> section are one to one copy of those you can find in JPublisher documentation. 
The only exception is <closeConnection> parameter. Setting it to true will modify generated classes so that after executing any stored procedure used connection will be closed - meaning it will be returned to the pool! 

##Datasource configuration

If you are using Spring you can make it as simply as:

```
<bean name="dataSource" class="oracle.jdbc.pool.OracleDataSource">
	<property name="URL" value="${database.url}"/>
	<property name="user" value="${database.user}"/>
	<property name="password" value="${database.password}"/>
	<property name="connectionCachingEnabled" value="true"/>
	<!--<property name="connectionCacheName" value="MYEXAMPLE_CACHE"/>-->
	<property name="implicitCachingEnabled" value="true"/>
	<property name="connectionCacheProperties">
		<props>
			<prop key="MinLimit">5</prop>
			<prop key="ConnectionCachingEnabled">True</prop>
			<prop key="InitialLimit">5</prop>
			<prop key="MaxLimit">5</prop>
			<prop key="ValidateConnection">false</prop>
			<!--<prop key="InactivityTimeout">10</prop>-->
			<prop key="AbandonedConnectionTimeout">180</prop>
			<prop key="ConnectionWaitTimeout">60</prop>
			<!--<prop key="TimeToLiveTimeout">5</prop>-->
			<!--<prop key="PropertyCheckInterval">300</prop>-->
		</props>
	</property>
</bean>
```

##Using generated classes

Oracle decided to make these generated classes NOT thread safe ( that's not bad), 
but with a little trick from Spring you can easily overcome this problem. 
Spring code snippet below assumes that I have class pl.touk.myexample.dao.oracle.MyExampleStoredProceduresPackage. Generated by JPublisher that contains methods to my stored procedures.
To use it as a bean in spring just do :

```
<bean id="targetMyExample" class="pl.touk.myexample.dao.oracle.MyExampleStoredProceduresPackage" scope="prototype">
	<property name="dataSource" ref="dataSource" />
</bean>

<bean id="threadTargetMyExample" class="org.springframework.aop.target.ThreadLocalTargetSource">
	<property name="targetBeanName" value="targetMyExample"/>
</bean>

<bean id="myExample" class="org.springframework.aop.framework.ProxyFactoryBean">
	<property name="targetSource" ref="threadTargetMyExample"/>
</bean>
```

I know this is not perfect, but it is good enough and it works very well. 
Good luck.
