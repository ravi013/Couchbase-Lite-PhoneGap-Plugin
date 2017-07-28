#import <Cordova/CDV.h>
#import "CBLite.h"
#import "CouchbaseLite.h"

@interface CBLite : CDVPlugin
    

@property (nonatomic, strong) NSURL *liteURL;
@property (nonatomic, strong) CBLDatabase *database;
@property (nonatomic, strong) CBLReplication *pull ;

- (void)getURL:(CDVInvokedUrlCommand*)urlCommand;
- (void) startReplication;

@end

