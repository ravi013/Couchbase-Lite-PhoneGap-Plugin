#import "CBLite.h"
#import "CouchbaseLite.h"
#import "CBLRegisterJSViewCompiler.h"

#import <Cordova/CDV.h>

@implementation CBLite

@synthesize liteURL;
@synthesize database;
@synthesize pull;
@synthesize listener;

- (void)pluginInitialize {
   
}

- (void)getURL:(CDVInvokedUrlCommand*)urlCommand
{
    CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsString:[self.liteURL absoluteString]];
    self.remoteUrl = urlCommand.arguments[0];
     [self launchCouchbaseLite];
    [self.commandDelegate sendPluginResult:pluginResult callbackId:urlCommand.callbackId];
}

- (void)launchCouchbaseLite
{
    NSError* error;

    NSLog(@"Launching Couchbase Lite...");
    CBLManager* dbmgr = [CBLManager sharedInstance];
    self.database = [dbmgr databaseNamed: @"fhs" error: &error];
    if (!self.database) {
        NSLog(@"Cannot create database instance: %@", error);
    }
    self.listener = [[CBLListener alloc] initWithManager:dbmgr port:55000];
    [listener start:nil];
 
    CBLRegisterJSViewCompiler();
#if 1
    // Couchbase Lite 1.0's CBLRegisterJSViewCompiler function doesn't register the filter compiler
    if ([CBLDatabase filterCompiler] == nil) {
        Class cblJSFilterCompiler = NSClassFromString(@"CBLJSFilterCompiler");
        [CBLDatabase setFilterCompiler: [[cblJSFilterCompiler alloc] init]];
    }
#endif
    self.liteURL = listener.URL;
    NSLog(@"Couchbase Lite url = %@", self.liteURL);
    [self startReplication];
}



-(void) startReplication
{
    NSURL* url = [NSURL URLWithString: self.remoteUrl];
   // CBLReplication *push = [database createPushReplication: url];
    CBLReplication *pull = [database createPullReplication: url];
    //push.continuous =
    pull.continuous = YES;
    id<CBLAuthenticator> auth;
    auth = [CBLAuthenticator basicAuthenticatorWithName: @"fshuser"
                                               password: @"fshuserqaz"];
    //push.authenticator =
    pull.authenticator = auth;
    self.pull=pull;
    [pull start];
    [[NSNotificationCenter defaultCenter] addObserver: self
                                             selector: @selector(replicationChanged:)
                                                 name: kCBLReplicationChangeNotification
                                               object: pull];
    NSLog(@"Starting replication = %@", self.liteURL);

    
}
- (void) replicationChanged: (NSNotification*)n {
    // The replication reporting the notification is n.object , but we
    // want to look at the aggregate of both the push and pull.
    
    // First check whether replication is currently active:
    BOOL active = (pull.status == kCBLReplicationActive) ;
    
    if (active) {
        double progress = 0.0;
        double total =  pull.changesCount;
        if (total > 0.0) {
            progress = (  pull.completedChangesCount) / total;
             NSLog(@" replication progress= %@",[NSString stringWithFormat:@"%f", progress]);
        }
       
    }else {
        NSError *error = pull.lastError;
      
            NSLog(@"Authentication error = %@",error);
        
    }
}
@end
