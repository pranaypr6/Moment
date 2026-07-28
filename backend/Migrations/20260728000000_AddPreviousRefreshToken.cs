using System;
using Microsoft.EntityFrameworkCore.Migrations;

#nullable disable

namespace Moment.Api.Migrations
{
    /// <inheritdoc />
    public partial class AddPreviousRefreshToken : Migration
    {
        /// <inheritdoc />
        protected override void Up(MigrationBuilder migrationBuilder)
        {
            migrationBuilder.AddColumn<string>(
                name: "PreviousRefreshToken",
                table: "Users",
                type: "text",
                nullable: true);

            migrationBuilder.AddColumn<DateTime>(
                name: "PreviousRefreshTokenExpiryTime",
                table: "Users",
                type: "timestamp with time zone",
                nullable: true);
        }

        /// <inheritdoc />
        protected override void Down(MigrationBuilder migrationBuilder)
        {
            migrationBuilder.DropColumn(
                name: "PreviousRefreshToken",
                table: "Users");

            migrationBuilder.DropColumn(
                name: "PreviousRefreshTokenExpiryTime",
                table: "Users");
        }
    }
}
